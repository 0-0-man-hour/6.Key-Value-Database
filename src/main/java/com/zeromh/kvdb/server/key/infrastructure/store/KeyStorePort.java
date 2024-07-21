package com.zeromh.kvdb.server.key.infrastructure.store;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.kvdb.server.common.domain.DataObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KeyStorePort {

    Mono<DataObject> getValue(HashKey key);

    Mono<DataObject> saveValue(HashKey key, DataObject dataObject);

    Flux<DataObject> getAllValue();
}
