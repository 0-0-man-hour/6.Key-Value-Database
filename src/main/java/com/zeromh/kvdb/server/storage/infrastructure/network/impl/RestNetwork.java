package com.zeromh.kvdb.server.storage.infrastructure.network.impl;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.storage.infrastructure.network.NetworkPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RestNetwork implements NetworkPort {

    private final static String REQUEST_KEY = "/key";
    private final static String REPLICA = "/replica";


    @Override
    public Mono<DataObject> fetchKeyValue(HashServer server, HashKey key, boolean isReplica) {
        if(!isReplica) {
            log.info("Redirect to {}", server.getName());
        }
        String path = REQUEST_KEY + (isReplica? REPLICA:"") + "/" + key.getKey();
        return webclientGenerator.get(server, path)
                .bodyToMono(DataObject.class);
    }

    @Override
    public Mono<Boolean> saveValue(HashServer server, DataObject dataObject, boolean isReplica) {
        if(!isReplica) {
            log.info("Redirect to {}", makeServerUrl(server));
        }
        var webclient = WebClient.builder().baseUrl(makeServerUrl(server)).build();
        return webclient.post()
                .uri(uriBuilder -> uriBuilder.path(isReplica?REQUEST_KEY + REPLICA : REQUEST_KEY).build())
                .bodyValue(dataObject)
                .retrieve()
                .bodyToMono(Boolean.class);    }


    private String makeServerUrl(HashServer server) {
        return "http://"+server.getUrl() + ":" + server.getPort();
    }
}
