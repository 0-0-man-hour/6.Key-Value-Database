package com.zeromh.kvdb.server.gossip.interfaces;

import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.gossip.application.GossipService;
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

    @PostMapping
    public Mono<Boolean> updateServerMembership(@RequestBody List<Membership> memberships) {
        return gossipService.updateHeartbeatList(memberships)
                .thenReturn(true);
    }

    @GetMapping
    public Flux<Membership> getServerMembershipList() {
        return gossipService.getServerMembershipList();
    }
}
