package com.zeromh.kvdb.server.gossip.infrastructure.network;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.Membership;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GossipNetworkPort {
    Mono<Boolean> propagateStatus(HashServer server, List<Membership> memberships);
}
