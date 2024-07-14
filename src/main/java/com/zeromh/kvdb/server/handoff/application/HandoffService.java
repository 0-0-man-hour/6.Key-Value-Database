package com.zeromh.kvdb.server.handoff.application;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.common.dto.HandoffDto;
import com.zeromh.kvdb.server.handoff.infrastructure.network.HandoffNetworkPort;
import com.zeromh.kvdb.server.handoff.infrastructure.store.HandoffStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandoffService {

    private final HandoffStorePort handoffStorePort;
    private final HandoffNetworkPort handoffNetworkPort;
    private final ServerManager serverManager;

    public Mono<Boolean> keepData(String serverName, DataObject dataObject) {
        return handoffStorePort.saveValue(serverName, dataObject)
                .thenReturn(true);
    }

    public Mono<Boolean> leaveData(String requestServer, String leaveServer, DataObject dataObject) {
        HashServer hashServer = serverManager.getServerByName(requestServer);
        HandoffDto handoffDto = HandoffDto.builder().dataObject(dataObject).serverName(leaveServer).build();
        return handoffNetworkPort.requestPostLeaveData(hashServer, handoffDto);
    }

    public Flux<DataObject> fetchLeftData(String serverName) {
        log.info("[Handoff] Request to fetch left data to {}", serverName);
        HashServer hashServer = serverManager.getServerByName(serverName);
        return handoffNetworkPort.requestGetLeftData(hashServer, serverManager.getMyServer());
    }

    public Flux<DataObject> getLeftData(String serverName) {
        return handoffStorePort.getAllValueAndRemove(serverName);
    }

}
