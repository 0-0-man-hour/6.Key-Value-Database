package com.zeromh.kvdb.server.gossip.application;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.common.domain.Status;
import com.zeromh.kvdb.server.common.domain.ServerMembership;
import com.zeromh.kvdb.server.gossip.infrastructure.network.GossipNetworkPort;
import com.zeromh.kvdb.server.common.util.DateUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GossipService {

    private final ServerManager serverManager;
    private final GossipNetworkPort gossipNetworkPort;
    private Map<String, ServerMembership> membershipMap;

    @Value("${gossip.threshold.temporary}")
    private long temporaryThresholdSeconds;
    @Value("${gossip.threshold.permanent}")
    private long permanentThresholdSeconds;

    @PostConstruct
    public void init() {
//        membershipMap = serverManager.getServerMap().values().stream()
//                .collect(Collectors.toMap(
//                        HashServer::getName,
//                        server -> ServerMembership.builder()
//                                .serverName(server.getName())
//                                .timeStamp(DateUtil.getTimeStamp())
//                                .status(Status.alive)
//                                .build()
//                ));

        membershipMap = new ConcurrentHashMap<>();
        serverManager.getServerMap().values()
                .forEach(server -> membershipMap.put(server.getName(),
                                ServerMembership.builder()
                                .serverName(server.getName())
                                .timeStamp(DateUtil.getTimeStamp())
                                .status(Status.alive)
                                .build()));
    }

    public Mono<Boolean> updateMyHeartbeat() {
        ServerMembership serverMembership = membershipMap.get(serverManager.getMyServer().getName());
        serverMembership.increaseHeartbeat();
        serverMembership.setTimeStamp(DateUtil.getTimeStamp());

        return Mono.just(true);
    }

    public Mono<Boolean> updateHeartbeat(ServerMembership requestMembership) {
        if (!membershipMap.containsKey(requestMembership.getServerName())) {
            membershipMap.put(requestMembership.getServerName(), requestMembership);
            return Mono.just(true);
        }
        ServerMembership saveMembership = membershipMap.get(requestMembership.getServerName());
        Status saveStatus = saveMembership.getStatus();
        if (saveMembership.isMoreUpToDateInfo(requestMembership)) {
            if (saveStatus.equals(Status.temporary) && !requestMembership.isNotUpdatedLongTime(temporaryThresholdSeconds)) {
                requestMembership.updateStatus(Status.alive);
                log.info("[Gossip] Recovered temporary failure of {}, failure time: {}", requestMembership.getServerName(), DateUtil.getDateTimeString(saveMembership.getTimeStamp()));
            }
            membershipMap.put(requestMembership.getServerName(), requestMembership);
            return Mono.just(true);
        }
        return Mono.just(false);
    }

    public Flux<Boolean> propagateStatus() {
        Random random = new Random();
        return Flux.fromIterable(serverManager.getServerMap().values())
                .filter(server -> !server.equals(serverManager.getMyServer()))
                .filter(server -> random.nextBoolean())
                .flatMap(server -> gossipNetworkPort.propagateStatus(server, membershipMap.values().stream().toList()));
    }

    public Flux<ServerMembership> findTemporaryFailureServer() {
        return Flux.fromIterable(membershipMap.values())
                .filter(membership -> membership.isNotUpdatedLongTime(temporaryThresholdSeconds) && membership.getStatus().equals(Status.alive))
                .doOnNext(membership -> log.info("[Gossip] Temporary failure has been detected on {}, last update time: {}", membership.getServerName(), DateUtil.getDateTimeString(membership.getTimeStamp())))
                .map(membership -> membership.updateStatus(Status.temporary));
    }

    public Flux<ServerMembership> findPermanentFailureServer() {
        return Flux.fromIterable(membershipMap.values())
                .filter(membership -> membership.isNotUpdatedLongTime(permanentThresholdSeconds) && membership.getStatus().equals(Status.temporary))
                .doOnNext(membership -> log.info("[Gossip] Permanent failure has been detected on {}, last update time: {}", membership.getServerName(), DateUtil.getDateTimeString(membership.getTimeStamp())))
                .map(membership -> membership.updateStatus(Status.permanent))
                .map(membership -> membershipMap.remove(membership.getServerName()));
    }

    public void deleteMembership(ServerMembership serverMembership) {
        membershipMap.remove(serverMembership.getServerName());
    }

    public Flux<ServerMembership> getServerMembershipList() {
        return Flux.fromIterable(membershipMap.values());
    }
}
