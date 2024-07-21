package com.zeromh.kvdb.server.config;

import com.zeromh.consistenthash.application.dto.ServerStatus;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ServerConfig {

    private final ServerProperty serverProperty;

    @Bean
    public HashServer myHashServer() {
        log.info("[Config] Server: {}", serverProperty.getName());
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
