package com.zeromh.kvdb.server.gossip.application;

import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.common.domain.Status;
import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.gossip.infrastructure.network.GossipNetworkPort;
import com.zeromh.kvdb.server.common.util.DateUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
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
    @Getter
    private Map<String, Membership> membershipMap;

    @Value("${gossip.threshold.temporary}")
    private long temporaryThresholdSeconds;
    @Value("${gossip.threshold.permanent}")
    private long permanentThresholdSeconds;

    @PostConstruct
    public void init() {
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

    public Flux<Boolean> updateHeartbeat(List<Membership> requestMemberships) {
        return Flux.fromIterable(requestMemberships)
                .filter(membership -> !membership.isNotUpdatedLongTime(permanentThresholdSeconds))
                .map(this::updateHeartbeatList);

    }

    private Boolean updateHeartbeatList(Membership requestMembership) {
        if (!membershipMap.containsKey(requestMembership.getServerName())) {
            membershipMap.put(requestMembership.getServerName(), requestMembership);
            return false;
        }

        Membership saveMembership = membershipMap.get(requestMembership.getServerName());
        if (requestMembership.isMoreUpToDateInfo(saveMembership)) {
            requestMembership.updateStatus(saveMembership.getStatus());
            membershipMap.put(requestMembership.getServerName(), requestMembership);
        }
        return true;
    }

    public Flux<Boolean> propagateStatus() {
        Random random = new Random();
        return Flux.fromIterable(serverManager.getServerMap().values())
                .filter(server -> !server.equals(serverManager.getMyServer()))
                .filter(server -> random.nextBoolean())
                .flatMap(server -> gossipNetworkPort.propagateStatus(server, membershipMap.values().stream().toList()));
    }


    public Flux<Membership> findRecoveredServer() {
        return Flux.fromIterable(serverManager.getServerMap().keySet())
                .mapNotNull(serverName -> membershipMap.get(serverName))
                .filter(membership -> membership.isRecovered(temporaryThresholdSeconds))
                .flatMap(membership -> gossipNetworkPort.checkServerHealth(serverManager.getMyServer(),  serverManager.getServerByName(membership.getServerName()))
                        .thenReturn(membership))
                .map(membership -> membership.updateStatus(Status.alive))
                .doOnNext(membership -> log.info("[Gossip] Recovered temporary failure of {}, failure time: {}", membership.getServerName(), DateUtil.getDateTimeString(membership.getTimeStamp())));
    }


    public Flux<Membership> findTemporaryFailureServer() {
        return Flux.fromIterable(serverManager.getServerMap().keySet())
                .mapNotNull(serverName -> membershipMap.get(serverName))
                .filter(membership -> membership.isNotUpdatedLongTime(temporaryThresholdSeconds) && membership.getStatus().equals(Status.alive))
                .doOnNext(membership -> log.info("[Gossip] Temporary failure has been detected on {}, last update time: {}", membership.getServerName(), DateUtil.getDateTimeString(membership.getTimeStamp())))
                .map(membership -> membership.updateStatus(Status.temporary));
    }

    public Flux<Membership> findPermanentFailureServer() {
        return Flux.fromIterable(serverManager.getServerMap().keySet())
                .mapNotNull(serverName -> membershipMap.get(serverName))
                .filter(membership -> membership.isNotUpdatedLongTime(permanentThresholdSeconds) && membership.getStatus().equals(Status.temporary))
                .doOnNext(membership -> log.info("[Gossip] Permanent failure has been detected on {}, last update time: {}", membership.getServerName(), DateUtil.getDateTimeString(membership.getTimeStamp())))
                .map(membership -> {
                    return membershipMap.remove(membership.getServerName());
                });
    }

    public void deleteMembership(Membership membership) {
        membershipMap.remove(membership.getServerName());
    }

    public Flux<Membership> getServerMembershipList() {
        return Flux.fromIterable(membershipMap.values());
    }
}
