package com.zeromh.kvdb.server.handoff.application;

import com.zeromh.kvdb.server.common.domain.DataObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HandoffUseCase {
    Mono<Boolean> keepData(String serverName, DataObject dataObject);

    Mono<Boolean> leaveData(String requestServer, String leaveServer, DataObject dataObject);

    Flux<DataObject> getAllLeftData(String serverName);
}
