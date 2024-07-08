package com.zeromh.kvdb.server.common;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Getter
public class ServerManager {
    private final ServerStatus serverStatus;
    private final HashServer myServer;
    private Map<String, HashServer> serverMap;

    @PostConstruct
    public void init() {
        //초기 서버 설정
        serverMap = serverStatus.getServerList().stream().collect(Collectors.toMap(
                HashServer::getName, server -> server
        ));
    }

    public HashServer getServerByName(String serverName) {
        return serverMap.get(serverName);
    }

    public HashServer addServer(HashServer server) {
        return serverMap.put(server.getName(), server);
    }

    public HashServer deleteServer(String serverName) {
        return serverMap.remove(serverName);
    }

}
