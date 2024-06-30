package com.zeromh.kvdb.server.application;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.kvdb.server.domain.DataObject;
import reactor.core.publisher.Mono;

public interface KeyUseCase {
    Mono<DataObject> getData(HashKey key);

    Mono<DataObject> getReplicaData(HashKey key);

    Mono<Boolean> saveData(DataObject dataObject);

    Mono<Boolean> saveReplicaData(DataObject dataObject);
}
