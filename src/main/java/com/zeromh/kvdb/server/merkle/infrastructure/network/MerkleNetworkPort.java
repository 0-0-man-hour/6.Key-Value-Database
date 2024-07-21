package com.zeromh.kvdb.server.merkle.infrastructure.network;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.merkle.domain.MerkleBucket;
import com.zeromh.kvdb.server.merkle.domain.MerkleTree;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface MerkleNetworkPort {


    Mono<Long> requestCompareMerkleHash(HashServer server, Long standardHash, MerkleBucket merkleBucket);


    Mono<List<String>> requestCompareMerkleBucketLeaf(HashServer server, Long standardHash, MerkleBucket merkleBucket);
}
