package com.zeromh.kvdb.server.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(value = "com.zeromh.consistenthash.domain.service")
public class ConsistentHashConfig {
}
