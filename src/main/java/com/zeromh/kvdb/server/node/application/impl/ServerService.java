package com.zeromh.kvdb.server.node.application.impl;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.consistenthash.domain.service.hash.HashServicePort;
import com.zeromh.kvdb.server.node.application.ServerUseCase;
import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.storage.infrastructure.network.NetworkPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServerService implements ServerUseCase {


    private final ServerManager serverManager;
    private final HashServicePort hashServicePort;
    private final NetworkPort networkPort;

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
    public HashServer getServer(HashKey key) {
        return null;
    }

}
