package com.zeromh.kvdb.server.node.interfaces;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.node.application.impl.NodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/node")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    public Mono<ServerStatus> getServerStatus(HashServer hashServer) {
        return nodeService.addServer(hashServer);
    }
}
