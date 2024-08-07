package com.zeromh.kvdb.server.gossip.interfaces;

import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.gossip.application.GossipUseCase;
import com.zeromh.kvdb.server.gossip.application.impl.GossipService;
import com.zeromh.kvdb.server.gossip.dto.GossipUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/gossip")
@RequiredArgsConstructor
public class GossipController {

    private final GossipUseCase gossipUseCase;

    @PostMapping
    public Mono<Membership> updateServerMembership(@RequestBody GossipUpdateDto gossipUpdateDto) {
        return gossipUseCase.updateHeartbeat(gossipUpdateDto);
    }

    @GetMapping
    public Flux<Membership> getServerMembershipList() {
        return gossipUseCase.getServerMembershipList();
    }

    @GetMapping("/health")
    public Mono<Boolean> checkHealth() {
        return Mono.just(true);
    }

    @GetMapping("/check")
    public Mono<Boolean> checkHealthServer(@RequestParam String serverName) {
        log.info("[Gossip] {} request to check server status", serverName);
        return Mono.just(gossipUseCase.checkServerHealth(serverName));
    }
}
