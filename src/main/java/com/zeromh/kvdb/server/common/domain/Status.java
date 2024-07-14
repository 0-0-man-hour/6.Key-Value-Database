package com.zeromh.kvdb.server.common.domain;

public enum Status {
    alive, temporary, permanent;

    public boolean isAlive() {
        return this == Status.alive;
    }
}
