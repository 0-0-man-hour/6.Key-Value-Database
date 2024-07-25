package com.zeromh.kvdb.server.node.application.impl;

import com.influxdb.client.write.Point;
import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.consistenthash.domain.service.hash.HashServicePort;
import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.common.infrastructure.monitoring.InfluxDBRepository;
import com.zeromh.kvdb.server.node.application.NodeUseCase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NodeService implements NodeUseCase {

    private final ServerManager serverManager;
    private final HashServicePort hashServicePort;
    private final InfluxDBRepository influxDBRepository;

    @PostConstruct
    public void init() {
        //초기 서버 설정
        Mono.just(serverManager.getServerList())
            .doOnNext(hashServicePort::setServer)
            .doOnNext(serverList -> log.info("[Node] {} servers registered.", serverList))
            .thenMany(Flux.fromIterable(hashServicePort.getServerHashes(serverManager.getMyServer())))
            .flatMap(hash -> influxDBRepository.writePoint(Point.measurement("hash_ring")
                    .addTag("node", serverManager.getMyServer().getName())
                    .addField("angle", hash)))
            .subscribe();
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

    @Override
    public Mono<Boolean> updateServerStatus(Membership membership) {
        HashServer updateServer = serverManager.getServerByName(membership.getServerName());
        List<HashServer> serverList = serverManager.updateServerStatus(updateServer, membership.getStatus().isAlive());
        hashServicePort.setServer(serverList);

        return Mono.just(true);
    }

    public void setServerStatus(Membership membership) {


    }

    @Override
    public Mono<ServerStatus> addServer(HashServer hashServer) {
        return null;
    }

    @Override
    public ServerStatus deleteServer(HashServer hashServer) {
        return null;
    }

}
