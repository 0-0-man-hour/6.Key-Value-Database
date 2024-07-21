package com.zeromh.kvdb.server.merkle.domain;

import com.zeromh.kvdb.server.common.domain.DataObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class MerkleTree {
    long standardHash;
    MerkleBucket root;

    long MAX_VALUE = Integer.MAX_VALUE * 2L;


    public MerkleTree(long start, long end) {
        this.standardHash = start;
        buildTree(start, end);
    }

    public long getMerkleValue() {
        return root.hashValue;
    }

    public long updateMerkleValue(long keyHash, DataObject val) {
        updateMerkle(root, keyHash, val);
        root.calculateHash();
        return root.hashValue;
    }


    private void updateMerkle(MerkleBucket now, long keyHash, DataObject value) {
        now = getLeafBucket(now, keyHash);
        now.updateLeaf(value);
        while(!now.equals(root)) {
            now.calculateHash();
            now = now.parent;
        }
        root.calculateHash();
    }

    public MerkleBucket getLeafBucket(MerkleBucket now, long keyHash) {
        while (!now.isLeafNode()) {
            long mid = (now.start + now.end) / 2;
            if (now.start > now.end) {
                mid = ((now.start + now.end + MAX_VALUE) % MAX_VALUE) / 2;
            }
            if (mid >= keyHash) {
                now = now.left;
            } else {
                now = now.right;
            }
        }

        return now;
    }

    public MerkleBucket getMiddleBucket(MerkleBucket now, long keyHash) {
        while (!now.isLeafNode()) {
            if (keyHash == now.start) break;

            long mid = (now.start + now.end) / 2;
            if (now.start > now.end) {
                mid = ((now.start + now.end + MAX_VALUE) % MAX_VALUE) / 2;
            }
            if (mid >= keyHash) {
                now = now.left;
            } else {
                now = now.right;
            }
        }

        return now;
    }

    public void buildTree(long start, long end) {
        boolean isChanged = start > end;
        int size = 1000;
        List<MerkleBucket> nodes = new ArrayList<>();
        if (isChanged) {
            end = end + MAX_VALUE;
            for (long i = start; i < end; i += size) {
                nodes.add(new MerkleBucket(i % MAX_VALUE, (i + size - 1) % MAX_VALUE));
            }

        } else {
            for (long i = start; i < end; i += size) {
                nodes.add(new MerkleBucket(i, Math.min(end-1, i + size - 1)));
            }
        }

        while (nodes.size() > 1) {
            List<MerkleBucket> newLevel = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i += 2) {
                if (i + 1 < nodes.size()) {
                    MerkleBucket parent = new MerkleBucket(nodes.get(i), nodes.get(i + 1));
                    nodes.get(i).setParent(parent);
                    nodes.get(i+1).setParent(parent);
                    newLevel.add(parent);
                } else {
                    newLevel.add(nodes.get(i));
                }
            }
            nodes = newLevel;
        }

        root = nodes.get(0);
    }

    public List<String> getNonMatchingNode(MerkleTree other) {
        List<String> nonMatchingKeys = new ArrayList<>();
        compareNodes(this.root, other.root, nonMatchingKeys);

        return nonMatchingKeys;
    }

    private void compareNodes(MerkleBucket node1, MerkleBucket node2, List<String> nonMatchingKeys) {
        if (node1.hashValue == node2.hashValue) {
            return;
        }

        if (node1.isLeafNode() && node2.isLeafNode()) {
            node1.addNonMatchingKey(node2.leafHashMap, nonMatchingKeys);
            return;
        }

        if (node1.left.hashValue != node2.left.hashValue) {
            compareNodes(node1.left, node2.left, nonMatchingKeys);
        }

        if (node1.right.hashValue != node2.right.hashValue) {
            compareNodes(node1.right, node2.right, nonMatchingKeys);
        }
    }
}