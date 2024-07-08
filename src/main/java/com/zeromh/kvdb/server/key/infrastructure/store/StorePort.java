package com.zeromh.kvdb.server.key.infrastructure.store;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.kvdb.server.common.domain.DataObject;
import reactor.core.publisher.Mono;

public interface StorePort {

    Mono<DataObject> getValue(HashKey key);

    Mono<DataObject> saveValue(HashKey key, DataObject dataObject);
}
