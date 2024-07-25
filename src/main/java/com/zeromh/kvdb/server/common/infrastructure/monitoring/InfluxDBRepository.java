package com.zeromh.kvdb.server.common.infrastructure.monitoring;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class InfluxDBRepository {

    private final InfluxDBClient influxDBClient;

    public Mono<Void> writePoint(Point point) {
        return Mono.fromRunnable(() -> influxDBClient.getWriteApiBlocking().writePoint(point));
    }

    @Scheduled(fixedRate = 1000*60*1000)
    public void test() {
//        Point row = Point.measurement("launcher_client_connection")
//                .addTag("privateIp","test")
//                .addTag("port", "0404")
//                .addField("clientCnt", 122);

        Point test = Point.measurement("handoff")
                .addTag("server", "test5")
                .addTag("from", "fromServer2")
                .addField("key", "testKey")
                .addField("command", "KEEP");
        writePoint(test).subscribe();
    }
}
