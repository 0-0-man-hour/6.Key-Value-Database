package com.zeromh.kvdb.server.common.domain;

import com.zeromh.kvdb.server.common.util.DateUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Builder
@Getter
@ToString
@Slf4j
public class Membership {
    private String serverName;
    private long heartbeat;
    @Setter
    private long timeStamp;
    private Status status;

    public void increaseHeartbeat() {
        heartbeat = heartbeat + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Membership that = (Membership) o;
        return Objects.equals(serverName, that.serverName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverName);
    }

    public boolean isNotUpdatedLongTime(long thresholdSeconds) {
        long diff = DateUtil.getTimeStamp() - timeStamp;
        if (diff > thresholdSeconds * 1000) {
            log.info("difference: {}, threshold: {}",DateUtil.getTimeStamp() - timeStamp, thresholdSeconds*1000);
        }
        return DateUtil.getTimeStamp() - timeStamp > thresholdSeconds * 1000;
    }

    public boolean isMoreUpToDateInfo(Membership membership) {
        return  this.getTimeStamp() > membership.getTimeStamp();
    }

    public Membership updateStatus(Status status) {
        this.status = status;
        return this;
    }
}
