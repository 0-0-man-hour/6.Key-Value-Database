package com.zeromh.kvdb.server.merkle.application;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.common.dto.MerkleHashDto;
import com.zeromh.kvdb.server.merkle.domain.MerkleTree;
import com.zeromh.kvdb.server.merkle.domain.dto.MerkleRequestDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MerkleUseCase {
    Mono<Void> updateMerkle(DataObject dataObject);

    MerkleTree getMerkleTreeOrCreateTree(MerkleHashDto merkleHashDto);

    Flux<String> checkTobeSameMerkle(HashServer requestServer);

    Mono<Long> getHashValue(MerkleRequestDto merkleRequestDto);

    Flux<String> compareMerkleAndFindDifferentKeys(MerkleRequestDto merkleRequestDto);
}
