package com.zeromh.kvdb.server.node.application;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.ServerMembership;
import reactor.core.publisher.Mono;

public interface NodeUseCase {
    Mono<ServerStatus> addServer(HashServer hashServer);

    ServerStatus deleteServer(HashServer hashServer);

    Mono<?>  deleteServer(ServerMembership serverMembership);
    HashServer getServer(HashKey key);

}