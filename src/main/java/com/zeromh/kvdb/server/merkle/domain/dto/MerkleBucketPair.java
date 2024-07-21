package com.zeromh.kvdb.server.merkle.domain.dto;

import com.zeromh.kvdb.server.merkle.domain.MerkleBucket;
import lombok.Getter;

@Getter
public class MerkleBucketPair {
    MerkleBucket left;
    MerkleBucket right;

    public MerkleBucketPair(MerkleBucket left, MerkleBucket right) {
        this.left = left;
        this.right = right;
    }

    public MerkleBucketPair(MerkleBucket now) {
        this.left = now.getLeft();
        this.right = now.getRight();
    }
}
