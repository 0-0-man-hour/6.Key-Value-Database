package com.zeromh.kvdb.server.gossip.interfaces;

import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.gossip.application.GossipService;
import com.zeromh.kvdb.server.node.application.NodeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GossipScheduler {
    private final GossipService gossipService;
    private final ServerManager serverManager;
    private final NodeUseCase nodeUseCase;

    @Scheduled(fixedRate = 1000*5, initialDelay = 1000*5)
    public void updateHeartbeat() {
        gossipService.updateMyHeartbeat()
                .thenMany(gossipService.findTemporaryFailureServer())
                .collectList()
                .flatMapMany(list -> gossipService.propagateStatus())
                .thenMany(gossipService.findPermanentFailureServer())
                .flatMap(nodeUseCase::deleteServer)
                .map(hashServer -> serverManager.deleteServer(hashServer.getName()))
                .subscribe();
    }

//    @Scheduled(fixedRate = 1000*10)
    public void checkServerFailure() {
        gossipService.findTemporaryFailureServer()
                .collectList()
                .flatMapMany(list -> gossipService.propagateStatus())
                .thenMany(gossipService.findPermanentFailureServer())
                .flatMap(nodeUseCase::deleteServer)
                .subscribe();
    }
}
