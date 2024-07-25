package com.zeromh.kvdb.server.gossip.application.impl;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.common.domain.Status;
import com.zeromh.kvdb.server.common.util.DateUtil;
import com.zeromh.kvdb.server.gossip.application.GossipUseCase;
import com.zeromh.kvdb.server.gossip.dto.GossipUpdateDto;
import com.zeromh.kvdb.server.gossip.infrastructure.network.GossipNetworkPort;
import com.zeromh.kvdb.server.handoff.infrastructure.network.HandoffNetworkPort;
import com.zeromh.kvdb.server.key.application.KeyUseCase;
import com.zeromh.kvdb.server.merkle.application.MerkleUseCase;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GossipService implements GossipUseCase {

    private final ServerManager serverManager;
    private final GossipNetworkPort gossipNetworkPort;
    private final HandoffNetworkPort handoffNetworkPort;
    private final KeyUseCase keyUseCase;
    private final MerkleUseCase merkleUseCase;

    @Getter
    private Map<String, Membership> membershipMap;

    @Value("${gossip.threshold.temporary}")
    private long temporaryThresholdSeconds;
    @Value("${gossip.threshold.permanent}")
    private long permanentThresholdSeconds;

    @PostConstruct
    public void init() throws InterruptedException {
        membershipMap = new ConcurrentHashMap<>();
        Collection<HashServer> servers = serverManager.getServerMap().values();
        servers.forEach(server -> membershipMap.put(server.getName(),
                Membership.builder()
                    .serverName(server.getName())
                    .timeStamp(DateUtil.getTimeStamp())
                    .status(Status.alive)
                .build()));
    }

    @Override
    public Mono<Boolean> updateMyHeartbeat() {
        Membership membership = membershipMap.get(serverManager.getMyServer().getName());
        membership.increaseHeartbeat();
        membership.setTimeStamp(DateUtil.getTimeStamp());

        return Mono.just(true);
    }

    @Override
    public Mono<Membership> updateHeartbeat(GossipUpdateDto gossipUpdateDto) {
        Membership requestServerMembership = membershipMap.get(gossipUpdateDto.getRequestServer()).copyMembership();

        return Flux.fromIterable(gossipUpdateDto.getMemberships())
                .filter(membership -> !membership.isNotUpdatedLongTime(permanentThresholdSeconds))
                .map(this::updateHeartbeatList)
                .then(Mono.just(requestServerMembership));

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

    @Override
    public Flux<String> propagateStatus() {
        Random random = new Random();
        return Flux.fromIterable(serverManager.getServerMap().values())
                .filter(server -> !server.equals(serverManager.getMyServer()))
                .filter(server -> random.nextBoolean())
                .flatMap(server -> gossipNetworkPort.propagateStatus(server, new GossipUpdateDto(serverManager.getMyServer().getName(), membershipMap.values().stream().toList())))
                .filter(myMembership -> !myMembership.getStatus().isAlive())
                .take(1)
                .doOnNext(membership -> log.info("[Gossip] server was in a state of failure."))
                .flatMap(membership -> fetchHandoffDataAndCheckMerkleValue());
    }

    private Flux<String> fetchHandoffDataAndCheckMerkleValue() {
        Collection<HashServer> servers = serverManager.getServerMap().values();
        return Flux.fromIterable(servers)
                .filter(server -> !server.equals(serverManager.getMyServer()))
                .flatMap(server -> handoffNetworkPort.requestGetLeftData(server, serverManager.getMyServer()))
                .flatMap(dataObject -> keyUseCase.saveData(HashKey.builder().key(dataObject.getKey()).build(), dataObject))
                .thenMany(Flux.fromIterable(servers))
                .flatMap(merkleUseCase::checkTobeSameMerkle);
    }

    @Override
    public Flux<Membership> findRecoveredServer() {
        return Flux.fromIterable(serverManager.getServerMap().keySet())
                .mapNotNull(serverName -> membershipMap.get(serverName))
                .filter(membership -> membership.isRecovered(temporaryThresholdSeconds))
                .flatMap(membership -> gossipNetworkPort.checkServerHealth(serverManager.getServerByName(membership.getServerName()))
                        .thenReturn(membership))
                .map(membership -> membership.updateStatus(Status.alive))
                .doOnNext(membership -> log.info("[Gossip] Recovered temporary failure of {}, failure time: {}", membership.getServerName(), DateUtil.getDateTimeString(membership.getTimeStamp())));
    }

    @Override
    public Flux<Membership> findTemporaryFailureServer() {
        return Flux.fromIterable(serverManager.getServerMap().keySet())
                .mapNotNull(serverName -> membershipMap.get(serverName))
                .filter(membership -> membership.isNotUpdatedLongTime(temporaryThresholdSeconds) && membership.getStatus().equals(Status.alive))
                .doOnNext(membership -> log.info("[Gossip] Temporary failure has been detected on {}, last update time: {}", membership.getServerName(), DateUtil.getDateTimeString(membership.getTimeStamp())))
                .map(membership -> membership.updateStatus(Status.temporary));
    }

    @Override
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

    @Override
    public Flux<Membership> getServerMembershipList() {
        return Flux.fromIterable(membershipMap.values());
    }

    @Override
    public boolean checkServerHealth(String serverName) {
        return membershipMap.get(serverName).getStatus().isAlive();
    }
}
