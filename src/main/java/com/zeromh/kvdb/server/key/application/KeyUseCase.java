package com.zeromh.kvdb.server.key.application;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.kvdb.server.common.domain.DataObject;
import reactor.core.publisher.Mono;

public interface KeyUseCase {
    Mono<DataObject> intermediateGetData(HashKey key);

    Mono<DataObject> getData(HashKey key);

    Mono<Boolean> intermediateSaveData(DataObject dataObject);

    Mono<DataObject> saveData(HashKey key, DataObject dataObject);
}
