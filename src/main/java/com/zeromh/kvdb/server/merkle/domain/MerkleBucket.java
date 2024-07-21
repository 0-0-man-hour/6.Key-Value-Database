package com.zeromh.kvdb.server.merkle.domain;

import com.zeromh.kvdb.server.common.domain.DataObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public class MerkleBucket {
    long start;
    long end;
    long hashValue;
    MerkleBucket left;
    MerkleBucket right;
    @Setter
    MerkleBucket parent;

    Map<String, Long> leafHashMap;

    public MerkleBucket(long start, long end) {
        this.start = start;
        this.end = end;
        this.leafHashMap = new HashMap<>();
    }

    public MerkleBucket(MerkleBucket left, MerkleBucket right) {
        this.left = left;
        this.right = right;
        this.start = left.start;
        this.end = right.end;
    }

    public void updateLeaf(DataObject value) {
        long hash = hash(value.getValue());
        leafHashMap.put(value.getKey(), hash);
    }

    public boolean isLeafNode() {
        return leafHashMap != null;
    }

    public void calculateHash() {
        long newValue = 0L;

        if (isLeafNode()) {
            for (Long hashValue : leafHashMap.values()) {
                newValue += hashValue;
            }
        } else {
            newValue = this.left.hashValue + this.right.hashValue;
        }
        hashValue = hash(newValue);
    }

    public long hash(Object value) {
        String data = value.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(data.getBytes());
            byte[] hashBytes = digest.digest();
            long h = 0L;

            for(int i = 0; i < 4; ++i) {
                h <<= 8;
                h |= (hashBytes[i] & 255);
            }

            return h;
        } catch (NoSuchAlgorithmException var2) {
            throw new IllegalStateException("no algorythm found");
        }
    }

    public void addNonMatchingKey(Map<String, Long> otherMap, List<String> nonMatchingKeys) {
        for(String key : leafHashMap.keySet()) {
            if (!otherMap.containsKey(key) || otherMap.get(key).equals(leafHashMap.get(key))) {
                nonMatchingKeys.add(key);
            }
        }
    }
}
