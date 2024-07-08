package com.zeromh.kvdb.server.node.application.impl;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.consistenthash.domain.service.hash.HashServicePort;
import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.node.application.NodeUseCase;
import com.zeromh.kvdb.server.common.ServerManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class NodeService implements NodeUseCase {


    private final ServerManager serverManager;
    private final HashServicePort hashServicePort;

    @PostConstruct
    public void init() {
        //초기 서버 설정
        Mono.just(serverManager.getServerStatus())
            .doOnNext(hashServicePort::setServer)
                .doOnNext(status -> log.info("{} servers registered.", status.getServerList()))
                .subscribe();
    }

    @Override
    public Mono<ServerStatus> addServer(HashServer hashServer) {
            return null;
    }

    @Override
    public ServerStatus deleteServer(HashServer hashServer) {
        return null;
    }

    @Override
    public Mono<HashServer> deleteServer(Membership membership) {
        HashServer deleteServer = serverManager.getServerByName(membership.getServerName());
        hashServicePort.deleteServerInfo(deleteServer);
        return Mono.just(deleteServer);
    }

    @Override
    public HashServer getServer(HashKey key) {
        return null;
    }

}
