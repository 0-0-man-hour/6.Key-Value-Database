package com.zeromh.kvdb.server.infrastructure.network.impl;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.kvdb.server.infrastructure.network.NetworkPort;
import com.zeromh.kvdb.server.domain.DataObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RestNetwork implements NetworkPort {

    private final static String REQUEST_KEY = "/key";
    private final static String REQUEST_SERVER = "/server";
    private final static String REPLICA = "/replica";


    @Override
    public Mono<DataObject> fetchKeyValue(HashServer server, HashKey key, boolean isReplica) {
        if(!isReplica) {
            log.info("Redirect to {}", makeServerUrl(server));
        }        var webclient = WebClient.builder().baseUrl(makeServerUrl(server)).build();
        return webclient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(isReplica?REQUEST_KEY + REPLICA : REQUEST_KEY)
                        .path("/"+key.getKey()).build())
                .retrieve()
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
