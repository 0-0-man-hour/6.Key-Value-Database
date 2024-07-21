package com.zeromh.kvdb.server.gossip.infrastructure.network;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.gossip.dto.GossipUpdateDto;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GossipNetworkPort {

    Mono<Membership> propagateStatus(HashServer server, GossipUpdateDto gossipUpdateDto);

    Mono<Boolean> checkServerHealth(HashServer server);

    Mono<Boolean> checkMyServerHealth(HashServer myServer, HashServer server);
}
