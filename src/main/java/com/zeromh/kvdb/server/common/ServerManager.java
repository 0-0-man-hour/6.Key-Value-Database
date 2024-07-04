package com.zeromh.kvdb.server.common;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.node.application.impl.ServerService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Getter
public class ServerManager {
    private final ServerStatus serverStatus;
    private final HashServer myServer;
    private final ServerService serverService;

    private List<HashServer> serverList;

    @PostConstruct
    public void init() {
        //초기 서버 설정
        serverList = new ArrayList<>(serverStatus.getServerList());
    }

    public boolean addServer(HashServer server) {
        return serverList.add(server);
    }

    public boolean deleteServer(HashServer server) {
        return serverList.remove(server);
    }

}
