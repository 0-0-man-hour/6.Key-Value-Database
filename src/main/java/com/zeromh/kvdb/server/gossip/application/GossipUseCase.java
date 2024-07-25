package com.zeromh.kvdb.server.gossip.application;

import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.gossip.dto.GossipUpdateDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GossipUseCase {
    Mono<Boolean> updateMyHeartbeat();

    Mono<Membership> updateHeartbeat(GossipUpdateDto gossipUpdateDto);

    Flux<String> propagateStatus();

    Flux<Membership> findRecoveredServer();

    Flux<Membership> findTemporaryFailureServer();

    Flux<Membership> findPermanentFailureServer();

    Flux<Membership> getServerMembershipList();

    boolean checkServerHealth(String serverName);
}
