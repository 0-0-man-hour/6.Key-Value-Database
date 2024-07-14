package com.zeromh.kvdb.server.gossip.interfaces;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.gossip.application.GossipService;
import com.zeromh.kvdb.server.handoff.application.HandoffService;
import com.zeromh.kvdb.server.key.application.KeyUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/gossip")
@RequiredArgsConstructor
public class GossipController {

    private final GossipService gossipService;
    private final HandoffService handoffService;
    private final KeyUseCase keyUseCase;

    @PostMapping
    public Mono<Boolean> updateServerMembership(@RequestBody List<Membership> memberships) {
        return gossipService.updateHeartbeat(memberships)
                .collectList()
                .thenReturn(true);
    }

    @GetMapping
    public Flux<Membership> getServerMembershipList() {
        return gossipService.getServerMembershipList();
    }

    @GetMapping("/health")
    public Mono<Boolean> checkHealth(@RequestParam String serverName) {
        log.info("[Gossip] {} request to check server status", serverName);
        handoffService.fetchLeftData(serverName)
                .flatMap(dataObject -> keyUseCase.saveData(HashKey.builder().key(dataObject.getKey()).build(), dataObject))
                .subscribe();

        return Mono.just(true);
    }
}
