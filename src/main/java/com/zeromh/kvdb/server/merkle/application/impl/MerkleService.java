package com.zeromh.kvdb.server.merkle.application.impl;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.consistenthash.domain.service.hash.HashServicePort;
import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.common.dto.MerkleHashDto;
import com.zeromh.kvdb.server.config.QuorumProperty;
import com.zeromh.kvdb.server.key.infrastructure.store.KeyStorePort;
import com.zeromh.kvdb.server.merkle.application.MerkleUseCase;
import com.zeromh.kvdb.server.merkle.domain.MerkleBucket;
import com.zeromh.kvdb.server.merkle.domain.MerkleTree;
import com.zeromh.kvdb.server.merkle.domain.dto.MerkleBucketPair;
import com.zeromh.kvdb.server.merkle.domain.dto.MerkleRequestDto;
import com.zeromh.kvdb.server.merkle.infrastructure.network.MerkleNetworkPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerkleService implements MerkleUseCase {
    private final Map<Long, MerkleTree> merkleTreeMap = new ConcurrentHashMap<>();
    private final MerkleNetworkPort merkleNetworkPort;
    private final ServerManager serverManager;
    private final HashServicePort hashServicePort;
    private final KeyStorePort keyStorePort;
    private final QuorumProperty quorumProperty;

    @PostConstruct
    public void init() {
        keyStorePort.getAllValue()
                .map(dataObject -> {
                    HashKey hashKey = HashKey.builder().key(dataObject.getKey()).build();
                    hashServicePort.getAliveServers(hashKey, quorumProperty.getNumberOfReplica());
                    dataObject.setMerkleHashDto(MerkleHashDto.fromHashKey(hashKey));
                    return dataObject;
                })
                .flatMap(this::updateMerkle)
                .subscribe();
    }

    @Override
    public Mono<Void> updateMerkle(DataObject dataObject) {
        if(dataObject.getMerkleHashDto() == null) {
            HashKey hashKey = HashKey.builder().key(dataObject.getKey()).build();
            hashServicePort.getAliveServers(hashKey, quorumProperty.getNumberOfReplica());
            dataObject.setMerkleHashDto(MerkleHashDto.fromHashKey(hashKey));
        }
        MerkleHashDto merkleHashDto = dataObject.getMerkleHashDto();
        MerkleTree merkleTree = getMerkleTreeOrCreateTree(merkleHashDto);

        long merkleValue = merkleTree.updateMerkleValue(merkleHashDto.getDataHash(), dataObject);
        return Mono.empty();
    }


    @Override
    public MerkleTree getMerkleTreeOrCreateTree(MerkleHashDto merkleHashDto) {
        if (!merkleTreeMap.containsKey(merkleHashDto.getStartNodeHash())) {
            MerkleTree merkleTree = new MerkleTree(merkleHashDto.getStartNodeHash(), merkleHashDto.getEndNodeHash());
            merkleTreeMap.put(merkleTree.getStandardHash(), merkleTree);
            log.info("[Merkle] Tree is created. hash: {}", merkleTree.getStandardHash());
        }

        return merkleTreeMap.get(merkleHashDto.getStartNodeHash());
    }

    @Override
    public Flux<String> checkTobeSameMerkle(HashServer requestServer) {
        List<String> differentKeys = new ArrayList<>();

        return Flux.fromIterable(merkleTreeMap.values())
                .filter(merkleTree -> {
                    List<HashServer> hashServers = hashServicePort.getServersFromHash(merkleTree.getStandardHash(), quorumProperty.getNumberOfReplica());
                    return hashServers.contains(requestServer);
                })
                .doOnNext(merkleTree -> log.info("[Merkle] compare tree({}) with {}'s value", merkleTree.getStandardHash() ,requestServer.getName()))
                .flatMap(merkleTree -> compareMerkleTreeWithOtherServer(requestServer, merkleTree)
                        .flatMap(merkleBucket -> merkleNetworkPort.requestCompareMerkleBucketLeaf(requestServer, merkleTree.getStandardHash(), merkleBucket))
                        .doOnNext(differentKeys::addAll)
                        .doOnComplete(() -> log.info("[Merkle] {} was compared with {}", merkleTree.getStandardHash() ,requestServer.getName()))
                )
                .thenMany(Flux.fromIterable(differentKeys));
    }

    private Flux<MerkleBucket> compareMerkleTreeWithOtherServer(HashServer requestServer, MerkleTree merkleTree) {
        ConcurrentLinkedDeque<MerkleBucketPair> deque = new ConcurrentLinkedDeque<>();
        List<MerkleBucket> differentBuckets = new ArrayList<>();
        MerkleBucket root = merkleTree.getRoot();
        return merkleNetworkPort.requestCompareMerkleHash(requestServer, merkleTree.getStandardHash(), root)
                .filter(hash -> hash.equals(root.getHashValue()))
                .doOnNext(hash -> deque.add(new MerkleBucketPair(root)))
                .thenMany(Flux.defer(() -> {
                    if (deque.isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.just(deque.poll())
                            .expand(currentPair -> fetchNodes(requestServer, merkleTree, currentPair)
                            .flatMapMany(tuple -> {
                                long leftHash = tuple.getT1();
                                long rightHash = tuple.getT2();

                                if (leftHash != currentPair.getLeft().getHashValue()) {
                                    if (currentPair.getLeft().isLeafNode()) {
                                        differentBuckets.add(currentPair.getLeft());
                                    } else {
                                        deque.add(new MerkleBucketPair(currentPair.getLeft()));
                                    }
                                }

                                if (rightHash != currentPair.getRight().getHashValue()) {
                                    if (currentPair.getRight().isLeafNode()) {
                                        differentBuckets.add(currentPair.getRight());
                                    } else {
                                        deque.add(new MerkleBucketPair(currentPair.getRight()));
                                    }
                                }

                                return Flux.fromIterable(deque);
                            }));
                        }
                    ))
                .thenMany(Flux.fromIterable(differentBuckets));
    }

    private Mono<Tuple2<Long, Long>> fetchNodes(HashServer hashServer, MerkleTree merkleTree, MerkleBucketPair pair) {
        Mono<Long> leftHash = merkleNetworkPort.requestCompareMerkleHash(hashServer, merkleTree.getStandardHash(), pair.getLeft());
        Mono<Long> rightHash = merkleNetworkPort.requestCompareMerkleHash(hashServer, merkleTree.getStandardHash(), pair.getRight());
        return Mono.zip(leftHash, rightHash);
    }

    @Override
    public Mono<Long> getHashValue(MerkleRequestDto merkleRequestDto) {
        MerkleTree merkleTree = merkleTreeMap.get(merkleRequestDto.getStandardHash());

        MerkleBucket merkleBucket = merkleTree.getMiddleBucket(merkleTree.getRoot(), merkleRequestDto.getStart());

        return Mono.just(merkleBucket.getHashValue());
    }

    @Override
    public Flux<String> compareMerkleAndFindDifferentKeys(MerkleRequestDto merkleRequestDto) {
        List<String> differentKey = new ArrayList<>();

        MerkleTree merkleTree = merkleTreeMap.get(merkleRequestDto.getStandardHash());
        MerkleBucket merkleBucket = merkleTree.getLeafBucket(merkleTree.getRoot(), merkleRequestDto.getStart());

        merkleBucket.addNonMatchingKey(merkleRequestDto.getMerkleBucket().getLeafHashMap(), differentKey);
        return Flux.fromIterable(differentKey);
    }
}
