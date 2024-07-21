package com.zeromh.kvdb.server.gossip.interfaces;

import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.gossip.application.GossipService;
import com.zeromh.kvdb.server.node.application.impl.NodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GossipScheduler {
    private final GossipService gossipService;
    private final ServerManager serverManager;
    private final NodeService nodeService;

    @Scheduled(fixedRate = 1000*5, initialDelay = 1000*5)
    public void updateHeartbeat() {
        gossipService.updateMyHeartbeat()
                .thenMany(gossipService.findRecoveredServer())
                .flatMap(nodeService::updateServerStatus)
                .thenMany(gossipService.findTemporaryFailureServer())
                .flatMap(nodeService::updateServerStatus)
                .thenMany(gossipService.propagateStatus())
                .thenMany(gossipService.findPermanentFailureServer())
                .flatMap(nodeService::deleteServer)
                .map(hashServer -> serverManager.deleteServer(hashServer.getName()))
                .subscribe();
    }
}
