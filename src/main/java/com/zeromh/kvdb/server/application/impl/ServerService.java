package com.zeromh.kvdb.server.application.impl;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.consistenthash.domain.service.hash.HashServicePort;
import com.zeromh.kvdb.server.application.ServerUseCase;
import com.zeromh.kvdb.server.infrastructure.network.NetworkPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServerService implements ServerUseCase {


    private final HashServer myHashServer;
    private final ServerStatus serverStatus;

    private final HashServicePort hashServicePort;
    private final NetworkPort networkPort;

    @PostConstruct
    public void init() {
        //초기 서버 설정
        Mono.just(serverStatus)
            .doOnNext(hashServicePort::setServer)
                .doOnNext(status -> log.info("{} servers registered.", serverStatus.getServerList()))
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
