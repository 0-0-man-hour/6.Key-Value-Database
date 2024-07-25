package com.zeromh.kvdb.server.handoff.application.impl;

import com.influxdb.client.write.Point;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.common.dto.HandoffDto;
import com.zeromh.kvdb.server.common.infrastructure.monitoring.InfluxDBRepository;
import com.zeromh.kvdb.server.handoff.application.HandoffUseCase;
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
public class HandoffService implements HandoffUseCase {

    private final HandoffStorePort handoffStorePort;
    private final HandoffNetworkPort handoffNetworkPort;
    private final ServerManager serverManager;
    private final InfluxDBRepository influxDBRepository;

    @Override
    public Mono<Boolean> keepData(String serverName, DataObject dataObject) {
        return handoffStorePort.saveValue(serverName, dataObject)
                .flatMap(dataObject1 -> influxDBRepository.writePoint(Point.measurement("handoff")
                        .addTag("server", serverManager.getMyServer().getName())
                        .addTag("from", serverName)
                        .addField("key", dataObject.getKey())
                        .addField("command", "KEEP")
                ))
                .thenReturn(true);
    }

    @Override
    public Mono<Boolean> leaveData(String requestServer, String leaveServer, DataObject dataObject) {
        HashServer hashServer = serverManager.getServerByName(requestServer);
        HandoffDto handoffDto = HandoffDto.builder().dataObject(dataObject).serverName(leaveServer).build();
        return handoffNetworkPort.requestPostLeaveData(hashServer, handoffDto);
    }

    @Override
    public Flux<DataObject> getAllLeftData(String serverName) {
        return handoffStorePort.getAllValueAndRemove(serverName)
                .flatMap(dataObject -> influxDBRepository.writePoint(Point.measurement("handoff")
                        .addTag("server", serverManager.getMyServer().getName())
                        .addTag("from", serverName)
                        .addField("key", dataObject.getKey())
                        .addField("command", "DELETE")
                ).thenReturn(dataObject));
    }

}
