package com.zeromh.kvdb.server.common.dto;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class MerkleHashDto {
    long dataHash;
    long startNodeHash;
    long endNodeHash;

    public static MerkleHashDto fromHashKey(HashKey key) {
        return MerkleHashDto.builder()
                .dataHash(key.getHashVal())
                .startNodeHash(key.getPrevServerHash())
                .endNodeHash(key.getServerHash())
                .build();
    }
}
