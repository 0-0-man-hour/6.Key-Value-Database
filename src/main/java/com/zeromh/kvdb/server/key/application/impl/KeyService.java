package com.zeromh.kvdb.server.key.application.impl;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.consistenthash.domain.service.hash.HashServicePort;
import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.common.domain.VectorClock;
import com.zeromh.kvdb.server.gossip.application.GossipService;
import com.zeromh.kvdb.server.key.application.KeyUseCase;
import com.zeromh.kvdb.server.key.infrastructure.network.KeyNetworkPort;
import com.zeromh.kvdb.server.config.QuorumProperty;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.key.infrastructure.store.StorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyService implements KeyUseCase {

    private final ServerManager serverManager;
    private final HashServicePort hashServicePort;
    private final QuorumProperty quorumProperty;

    private final GossipService gossipService;
    private final KeyNetworkPort keyNetworkPort;
    private final StorePort storePort;

    @Override
    public Mono<DataObject> intermediateGetData(HashKey key) {
        List<HashServer> targetServers = new ArrayList<>(hashServicePort.getServers(key, quorumProperty.getNumberOfReplica()));
        if (targetServers.contains(serverManager.getMyServer())) {
            return Flux.fromIterable(targetServers)
                    .flatMap(server -> {
                        if (server.equals(serverManager.getMyServer())) return getData(key)
                                .zipWith(Mono.just(server));
                        return keyNetworkPort.fetchKeyValue(server, key, true)
                                .zipWith(Mono.just(server));
                    })
                    .take(quorumProperty.getRead())
                    .doOnNext(tuple2 -> log.info("[Key] Ack of putting {} came from {} ", key.getKey() ,tuple2.getT2().getName()))
                    .collectList()
                    .flatMap(tuple2 -> checkConflictAndSendLatestVersion(key, tuple2));
        }

        targetServers.sort(Comparator.comparing((HashServer server) -> gossipService.getMembershipMap().get(server.getName()).getTimeStamp())
                .reversed());
        return fetchDataFromServers(targetServers, key, 0);

    }


    private Mono<DataObject> fetchDataFromServers(List<HashServer> servers, HashKey key, int index) {
        if (index >= servers.size()) {
            return Mono.error(new RuntimeException("All servers failed"));
        }
        HashServer currentServer = servers.get(index);
        log.info("[Key] {} is selected as the coordinator server for GET", currentServer.getName());
        return keyNetworkPort.fetchKeyValue(currentServer, key, false)
                .onErrorResume(error -> fetchDataFromServers(servers, key, index + 1));
    }

    @Override
    public Mono<DataObject> getData(HashKey key) {
        return storePort.getValue(key);
    }

    private Mono<DataObject> checkConflictAndSendLatestVersion(HashKey key, List<Tuple2<DataObject, HashServer>> tupleData) {
        Tuple2<DataObject, HashServer> latest = tupleData.get(0);
        boolean isConflict = false;
        for (Tuple2<DataObject, HashServer> item : tupleData) {
            VectorClock.ComparisonResult result = latest.getT1().getVectorClock().compare(item.getT1().getVectorClock());
            if (result == VectorClock.ComparisonResult.BEFORE) {
                latest = item;
                isConflict = true;
            }
        }

        if (isConflict) {
            latest.getT1().getVectorClock().tick(serverManager.getMyServer().getName());
            log.info("[Key] resolve conflict data. update key: {}", latest.getT1().getKey());
            Tuple2<DataObject, HashServer> finalLatest = latest;
            return Flux.fromIterable(tupleData)
                    .flatMap(dataObject -> {
                        if (dataObject.getT2().equals(serverManager.getMyServer())) {
                            return storePort.saveValue(key, finalLatest.getT1());
                        }
                        return keyNetworkPort.saveValue(dataObject.getT2(), finalLatest.getT1(), true);
                    })
                    .then(Mono.just(latest.getT1()));
        }
        return Mono.just(latest.getT1());
    }

    @Override
    public Mono<Boolean> intermediateSaveData(DataObject dataObject) {
        HashKey key = HashKey.builder().key(dataObject.getKey()).build();
        List<HashServer> targetServers = new ArrayList<>(hashServicePort.getServers(key, quorumProperty.getNumberOfReplica()));
        if (targetServers.contains(serverManager.getMyServer())) {
            return storePort.getValue(key)
                    .defaultIfEmpty(dataObject)
                    .map(save -> createVectorClock(save, dataObject))
                    .thenMany(Flux.fromIterable(hashServicePort.getServers(key, quorumProperty.getNumberOfReplica())))
                    .flatMap(server -> {
                        if (server.equals(serverManager.getMyServer())) return saveData(key, dataObject)
                                .zipWith(Mono.just(server));
                        return keyNetworkPort.saveValue(server, dataObject, true)
                                .zipWith(Mono.just(server));
                    })
                    .take(quorumProperty.getWrite())
                    .doOnNext(tuple2 -> log.info("[Key] Ack of putting {} came from {} ", key.getKey() ,tuple2.getT2().getName()))
                    .then(Mono.just(true));
        }

        targetServers.sort(Comparator.comparing((HashServer server) -> gossipService.getMembershipMap().get(server.getName()).getTimeStamp())
                .reversed());
        return saveDataToServers(targetServers, dataObject, 0);
    }

    private Mono<Boolean> saveDataToServers(List<HashServer> servers, DataObject dataObject, int index) {
        if (index >= servers.size()) {
            return Mono.error(new RuntimeException("All servers failed to save the data"));
        }

        HashServer currentServer = servers.get(index);
        log.info("[Key] {} is selected as the coordinator server for SAVE", currentServer.getName());
        return keyNetworkPort.saveValue(currentServer, dataObject, false)
                .onErrorResume(error -> saveDataToServers(servers, dataObject, index + 1));
    }

    private DataObject createVectorClock(DataObject save, DataObject request) {
        VectorClock saveVectorClock = save.getVectorClock();
        if (saveVectorClock == null) {
            VectorClock vectorClock = new VectorClock();
            vectorClock.tick(serverManager.getMyServer().getName());
            request.setVectorClock(vectorClock);
        } else {
            saveVectorClock.tick(serverManager.getMyServer().getName());
            request.setVectorClock(saveVectorClock);
        }


        return request;
    }

    @Override
    public Mono<DataObject> saveData(HashKey key, DataObject request) {
        return storePort.saveValue(key, request);
    }
}
