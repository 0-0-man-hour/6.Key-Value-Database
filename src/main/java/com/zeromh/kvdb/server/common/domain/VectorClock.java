package com.zeromh.kvdb.server.common.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@NoArgsConstructor
public class VectorClock {

    private Map<String, Integer> clock = new HashMap<>();


    public void tick(String serverName) {
        clock.put(serverName, clock.getOrDefault(serverName, 0) + 1);
    }

    public void update(VectorClock other) {
        for (Map.Entry<String, Integer> entry : other.clock.entrySet()) {
            String processId = entry.getKey();
            int otherTime = entry.getValue();
            int currentTime = clock.getOrDefault(processId, 0);
            clock.put(processId, Math.max(currentTime, otherTime));
        }
    }

    public ComparisonResult compare(VectorClock other) {
        boolean lessThan = false;
        boolean greaterThan = false;

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(clock.keySet());
        allKeys.addAll(other.clock.keySet());

        for (String processId : allKeys) {
            int thisTime = clock.getOrDefault(processId, 0);
            int otherTime = other.clock.getOrDefault(processId, 0);

            if (thisTime < otherTime) {
                lessThan = true;
            }
            if (thisTime > otherTime) {
                greaterThan = true;
            }
        }

        if (lessThan && greaterThan) {
            return ComparisonResult.CONCURRENT;
        } else if (lessThan) {
            return ComparisonResult.BEFORE;
        } else if (greaterThan) {
            return ComparisonResult.AFTER;
        } else {
            return ComparisonResult.EQUAL;
        }
    }

    public enum ComparisonResult {
        BEFORE,
        AFTER,
        CONCURRENT,
        EQUAL
    }

}
