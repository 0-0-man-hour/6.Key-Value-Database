package com.zeromh.kvdb.server.node.interfaces;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.node.application.ServerUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/server")
@RequiredArgsConstructor
public class ServerController {

    private final ServerUseCase serverUseCase;
    public Mono<ServerStatus> getServerStatus(HashServer hashServer) {
        return serverUseCase.addServer(hashServer);
    }
}
