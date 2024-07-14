package com.zeromh.kvdb.server.gossip.infrastructure.network.impl;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.Membership;
import com.zeromh.kvdb.server.common.infrastructure.WebclientGenerator;
import com.zeromh.kvdb.server.gossip.infrastructure.network.GossipNetworkPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GossipRestNetwork implements GossipNetworkPort {
    private final static String REQUEST_GOSSIP = "/gossip";
    private final WebclientGenerator webclientGenerator;


    @Override
    public Mono<Boolean> propagateStatus(HashServer server, List<Membership> memberships) {
        return webclientGenerator.post(server, REQUEST_GOSSIP, memberships)
                .bodyToMono(Boolean.class)
                .doOnError(e -> log.error("[Gossip] Server: {}, request failed, message: {}", server.getName(), e.getMessage()))
                .onErrorComplete();
    }

    @Override
    public Mono<Boolean> checkServerHealth(HashServer myServer, HashServer server) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("serverName", myServer.getName());
        return webclientGenerator.get(server, REQUEST_GOSSIP + "/health", params)
                .bodyToMono(Boolean.class);
    }

}
