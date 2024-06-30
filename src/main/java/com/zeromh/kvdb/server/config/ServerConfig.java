package com.zeromh.kvdb.server.config;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ServerConfig {

    private final ServerProperty serverProperty;

    @Bean
    public HashServer myHashServer() {
        log.info(serverProperty.getName());
        return HashServer.builder()
                .name(serverProperty.getName())
                .port(serverProperty.getPort())
                .url(serverProperty.getUrl())
                .numsOfNode(serverProperty.getNumsOfNode())
                .build();
    }

    @Bean
    public ServerStatus serverStatus() {
        return ServerStatus.builder()
                .serverList(
                        serverProperty.getServerList().stream()
                                .map(serverString -> serverString.split(":"))
                                .map(serverInfoArray -> HashServer.builder()
                                        .name(serverInfoArray[0])
                                        .url(serverInfoArray[1])
                                        .port(serverInfoArray[2])
                                        .numsOfNode(Integer.parseInt(serverInfoArray[3]))
                                        .build()
                                ).toList())
                .build();

    }
}
