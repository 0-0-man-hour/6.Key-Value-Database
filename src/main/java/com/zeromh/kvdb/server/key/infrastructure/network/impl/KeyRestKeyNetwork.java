package com.zeromh.kvdb.server.key.infrastructure.network.impl;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.common.infrastructure.WebclientGenerator;
import com.zeromh.kvdb.server.key.infrastructure.network.KeyNetworkPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyRestKeyNetwork implements KeyNetworkPort {

    private final static String REQUEST_KEY = "/key";
    private final static String REPLICA = "/replica";

    private final WebclientGenerator webclientGenerator;

    @Override
    public Mono<DataObject> fetchKeyValue(HashServer server, HashKey key, boolean isReplica) {
        if(!isReplica) {
            log.info("[Key] Redirect to {}", server.getName());
        }
        String path = REQUEST_KEY + (isReplica? REPLICA:"") + "/" + key.getKey();
        return webclientGenerator.get(server, path)
                .bodyToMono(DataObject.class);
    }

    @Override
    public Mono<Boolean> saveValue(HashServer server, DataObject dataObject, boolean isReplica) {
        if (!isReplica) {
            log.info("[Key] Redirect to {}", server.getName());
        }
        String path = REQUEST_KEY + (isReplica ? REPLICA : "");
        return webclientGenerator.post(server, path, dataObject)
                .bodyToMono(Boolean.class);
    }


}
