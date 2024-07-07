package com.zeromh.kvdb.server.key.infrastructure.network;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.DataObject;
import reactor.core.publisher.Mono;

public interface KeyNetworkPort {
//    Mono<ServerStatus> fetchServerStatus(HashServer server);

    Mono<DataObject> fetchKeyValue(HashServer server, HashKey key, boolean isReplica);

    Mono<Boolean> saveValue(HashServer server, DataObject requestDto, boolean isReplica);
}
