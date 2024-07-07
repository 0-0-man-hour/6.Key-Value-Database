package com.zeromh.kvdb.server.gossip.application;

import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.common.domain.Status;
import com.zeromh.kvdb.server.common.domain.Membership;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class GossipService {

    private final ServerManager serverManager;
    private final GossipNetworkPort gossipNetworkPort;
    private Map<String, Membership> membershipMap;

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
                                Membership.builder()
                                .serverName(server.getName())
                                .timeStamp(DateUtil.getTimeStamp())
                                .status(Status.alive)
                                .build()));
    }

    public Mono<Boolean> updateMyHeartbeat() {
        Membership membership = membershipMap.get(serverManager.getMyServer().getName());
        membership.increaseHeartbeat();
        membership.setTimeStamp(DateUtil.getTimeStamp());

        return Mono.just(true);
    }

    public Mono<Boolean> updateHeartbeat(Membership requestMembership) {
        if (!membershipMap.containsKey(requestMembership.getServerName())) {
            membershipMap.put(requestMembership.getServerName(), requestMembership);
            return Mono.just(true);
        }
        Membership saveMembership = membershipMap.get(requestMembership.getServerName());
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

    public Flux<Membership> findTemporaryFailureServer() {
        return Flux.fromIterable(membershipMap.values())
                .filter(membership -> membership.isNotUpdatedLongTime(temporaryThresholdSeconds) && membership.getStatus().equals(Status.alive))
                .doOnNext(membership -> log.info("[Gossip] Temporary failure has been detected on {}, last update time: {}", membership.getServerName(), DateUtil.getDateTimeString(membership.getTimeStamp())))
                .map(membership -> membership.updateStatus(Status.temporary));
    }

    public Flux<Membership> findPermanentFailureServer() {
        return Flux.fromIterable(membershipMap.values())
                .filter(membership -> membership.isNotUpdatedLongTime(permanentThresholdSeconds) && membership.getStatus().equals(Status.temporary))
                .doOnNext(membership -> log.info("[Gossip] Permanent failure has been detected on {}, last update time: {}", membership.getServerName(), DateUtil.getDateTimeString(membership.getTimeStamp())))
                .map(membership -> membership.updateStatus(Status.permanent))
                .map(membership -> membershipMap.remove(membership.getServerName()));
    }

    public void deleteMembership(Membership membership) {
        membershipMap.remove(membership.getServerName());
    }

    public Flux<Membership> getServerMembershipList() {
        return Flux.fromIterable(membershipMap.values());
    }
}
