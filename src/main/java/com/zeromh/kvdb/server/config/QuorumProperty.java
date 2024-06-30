package com.zeromh.kvdb.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "quorum")
public class QuorumProperty {
    private int numberOfReplica;
    private int write;
    private int read;
}
