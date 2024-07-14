package com.zeromh.kvdb.server.common;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.config.ServerProperty;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Getter
public class ServerManager {
    private final HashServer myServer;
    private final ServerProperty serverProperty;

    private Map<String, HashServer> serverMap;

    @PostConstruct
    public void init() {
        //초기 서버 설정
        serverMap = new ConcurrentHashMap<>();
        serverProperty.getServerList().stream()
                .map(serverString -> serverString.split(":"))
                .forEach(serverInfoArray -> serverMap.put(serverInfoArray[0], HashServer.builder()
                        .name(serverInfoArray[0])
                        .url(serverInfoArray[1])
                        .port(serverInfoArray[2])
                        .numsOfNode(Integer.parseInt(serverInfoArray[3]))
                        .isAlive(true)
                        .build()
                        )
                );
    }

    public List<HashServer> getServerList() {
        return serverMap.values().stream().toList();
    }

    public HashServer getServerByName(String serverName) {
        return serverMap.get(serverName);
    }

    public List<HashServer> addServer(HashServer server) {
        serverMap.put(server.getName(), server);
        return getServerList() ;
    }

    public List<HashServer> deleteServer(String serverName) {
        serverMap.remove(serverName);
        return getServerList() ;
    }

    public List<HashServer> updateServerStatus(HashServer server, boolean isAlive) {
        server.setAlive(isAlive);
        return getServerList();
    }
}
