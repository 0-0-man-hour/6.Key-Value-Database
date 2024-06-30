package com.zeromh.kvdb.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerProperty {
    private String name;
    private String url;
    private String port;
    private int numsOfNode;
    private List<String> serverList;
}
