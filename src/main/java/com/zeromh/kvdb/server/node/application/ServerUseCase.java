package com.zeromh.kvdb.server.node.application;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import reactor.core.publisher.Mono;

public interface ServerUseCase {
    Mono<ServerStatus> addServer(HashServer hashServer);

    ServerStatus deleteServer(HashServer hashServer);

    HashServer getServer(HashKey key);

}
