package com.zeromh.kvdb.server.gossip.interfaces;

import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.gossip.application.GossipUseCase;
import com.zeromh.kvdb.server.node.application.NodeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GossipScheduler {
    private final GossipUseCase gossipUseCase;
    private final ServerManager serverManager;
    private final NodeUseCase nodeUseCase;

    @Scheduled(fixedRate = 1000*5, initialDelay = 1000*5)
    public void updateHeartbeat() {
        gossipUseCase.updateMyHeartbeat()
                .thenMany(gossipUseCase.findRecoveredServer())
                .flatMap(nodeUseCase::updateServerStatus)
                .thenMany(gossipUseCase.findTemporaryFailureServer())
                .flatMap(nodeUseCase::updateServerStatus)
                .thenMany(gossipUseCase.propagateStatus())
                .thenMany(gossipUseCase.findPermanentFailureServer())
                .flatMap(nodeUseCase::deleteServer)
                .map(hashServer -> serverManager.deleteServer(hashServer.getName()))
                .subscribe();
    }
}
