package com.zeromh.kvdb.server.merkle.domain.dto;

import com.zeromh.kvdb.server.merkle.domain.MerkleBucket;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MerkleRequestDto {
    Long standardHash;
    Long start;
    Long end;
    MerkleBucket merkleBucket;

    public MerkleRequestDto(long standardHash, MerkleBucket merkleBucket) {
        this.standardHash = standardHash;
        this.merkleBucket = merkleBucket;
    }
}
