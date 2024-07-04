package com.zeromh.kvdb.server.common.infrastructure;

import com.zeromh.consistenthash.domain.model.server.HashServer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import java.util.List;

@Component
public class WebclientGenerator {

    public ResponseSpec get(HashServer server, String path) {
        var webclient = WebClient.builder().baseUrl(makeServerUrl(server)).build();
        return webclient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .retrieve();
    }

    public <T> ResponseSpec post(HashServer server, String path, T bodyValue) {
        var webclient = WebClient.builder().baseUrl(makeServerUrl(server)).build();
        return webclient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(bodyValue)
                .retrieve();
    }

    public <T> ResponseSpec post(HashServer server, String path, List<HashServer> bodyValue) {
        var webclient = WebClient.builder().baseUrl(makeServerUrl(server)).build();
        return webclient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(bodyValue)
                .retrieve();
    }


    private String makeServerUrl(HashServer server) {
        return "http://"+server.getUrl() + ":" + server.getPort();
    }

}
