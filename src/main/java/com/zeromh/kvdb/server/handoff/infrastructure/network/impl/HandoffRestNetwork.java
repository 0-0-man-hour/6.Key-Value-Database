package com.zeromh.kvdb.server.handoff.infrastructure.network.impl;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.common.dto.HandoffDto;
import com.zeromh.kvdb.server.common.infrastructure.WebclientGenerator;
import com.zeromh.kvdb.server.handoff.infrastructure.network.HandoffNetworkPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class HandoffRestNetwork implements HandoffNetworkPort {

    private final static String REQUEST_HANDOFF = "/handoff";
    private final WebclientGenerator webclientGenerator;

    @Override
    public Flux<DataObject> requestGetLeftData(HashServer server, HashServer myServer) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("serverName", myServer.getName());
        return webclientGenerator.get(server, REQUEST_HANDOFF, params)
                .bodyToFlux(DataObject.class)
                .onErrorComplete();
    }

    @Override
    public Mono<Boolean> requestPostLeaveData(HashServer server, HandoffDto handoffDto) {
        return webclientGenerator.post(server, REQUEST_HANDOFF, handoffDto)
                .bodyToMono(Boolean.class)
                .onErrorComplete();
    }
}
