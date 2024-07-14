package com.zeromh.kvdb.server.handoff.infrastructure.network;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.common.dto.HandoffDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HandoffNetworkPort {

    Flux<DataObject> requestGetLeftData(HashServer server, HashServer myServer);

    Mono<Boolean> requestPostLeaveData(HashServer server, HandoffDto handoffDto);
}
