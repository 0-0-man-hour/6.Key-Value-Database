package com.zeromh.kvdb.server.infrastructure.store;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.kvdb.server.domain.DataObject;
import reactor.core.publisher.Mono;

public interface StorePort {
    Mono<DataObject> getValue(HashKey key);
    Mono<Boolean> saveValue(HashKey hashKey, DataObject dataObject);

}
