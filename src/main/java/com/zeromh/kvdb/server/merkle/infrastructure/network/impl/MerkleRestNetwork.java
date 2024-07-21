package com.zeromh.kvdb.server.merkle.infrastructure.network.impl;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.infrastructure.WebclientGenerator;
import com.zeromh.kvdb.server.merkle.domain.MerkleBucket;
import com.zeromh.kvdb.server.merkle.domain.MerkleTree;
import com.zeromh.kvdb.server.merkle.domain.dto.MerkleRequestDto;
import com.zeromh.kvdb.server.merkle.infrastructure.network.MerkleNetworkPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerkleRestNetwork implements MerkleNetworkPort {
    private final static String REQUEST_MERKLE = "/merkle";
    private final WebclientGenerator webclientGenerator;


    @Override
    public Mono<Long> requestCompareMerkleHash(HashServer server, Long standardHash, MerkleBucket merkleBucket) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("standardHash", String.valueOf(standardHash));
        params.add("start", String.valueOf(merkleBucket.getStart()));
        params.add("end", String.valueOf(merkleBucket.getEnd()));

        return webclientGenerator.get(server, REQUEST_MERKLE, params)
                .bodyToMono(Long.class)
                .doOnError(e -> log.error("[Merkle] Server: {}, request failed, message: {}", server.getName(), e.getMessage()))
                .onErrorComplete();
    }

    @Override
    public Mono<List<String>> requestCompareMerkleBucketLeaf(HashServer server, Long standardHash, MerkleBucket merkleBucket) {
        MerkleRequestDto merkleRequestDto = new MerkleRequestDto(standardHash, merkleBucket);
        return webclientGenerator.post(server, REQUEST_MERKLE, merkleRequestDto)
                .bodyToFlux(String.class)
                .collectList()
                .doOnError(e -> log.error("[Merkle] Server: {}, request failed, message: {}", server.getName(), e.getMessage()))
                .onErrorComplete();
    }

}
