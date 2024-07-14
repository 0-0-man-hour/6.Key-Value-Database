package com.zeromh.kvdb.server.handoff.infrastructure.store;

import com.zeromh.kvdb.server.common.domain.DataObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HandoffStorePort {

    Flux<DataObject> getAllValueAndRemove(String serverName);

    Mono<DataObject> saveValue(String serverName, DataObject dataObject);
}
