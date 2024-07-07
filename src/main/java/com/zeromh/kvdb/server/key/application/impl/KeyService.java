package com.zeromh.kvdb.server.key.application.impl;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.consistenthash.domain.service.hash.HashServicePort;
import com.zeromh.kvdb.server.key.application.KeyUseCase;
import com.zeromh.kvdb.server.key.infrastructure.network.KeyNetworkPort;
import com.zeromh.kvdb.server.config.QuorumProperty;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.key.infrastructure.store.impl.MongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyService implements KeyUseCase {

    private final HashServer myHashServer;
    private final HashServicePort hashServicePort;
    private final QuorumProperty quorumProperty;

    private final KeyNetworkPort keyNetworkPort;
    private final MongoRepository mongoRepository;


    @Override
    public Mono<DataObject> getData(HashKey key) {
        HashServer server = hashServicePort.getServer(key);
        if (server.equals(myHashServer)) {
            log.info("[Key] myserver({}) is target of getting data(key={}).", server.getName(), key.getKey());
            return mongoRepository.getValue(key, false)
                    .flatMap(data ->  Flux.fromIterable(hashServicePort.getReplicaServers(key, quorumProperty.getNumberOfReplica()))
                            .flatMap(replicaServer -> keyNetworkPort.fetchKeyValue(replicaServer, key, true)
                                    .zipWith(Mono.just(replicaServer)))
                            .take(quorumProperty.getRead())
                            .filter(data::equals)
                            .doOnNext(tuple2 -> log.info("[Key] Ack of getting data came from {} ", tuple2.getT2().getName()))
                            .then(Mono.just(data))
                    );

        }
        return keyNetworkPort.fetchKeyValue(server, key, false);

    }

    @Override
    public Mono<DataObject> getReplicaData(HashKey key) {
        return mongoRepository.getValue(key, true);
    }

    @Override
    public Mono<Boolean> saveData(DataObject dataObject) {
        HashKey key = HashKey.builder().key(dataObject.getKey()).build();
        HashServer server = hashServicePort.getServer(key);
        if (server.equals(myHashServer)) {
            log.info("[Key] myserver({}) is target of putting data(key={}).", server.getName(), dataObject.getKey());
            return mongoRepository.saveValue(key, dataObject, false)
                    .flatMap(data ->  Flux.fromIterable(hashServicePort.getReplicaServers(key, quorumProperty.getNumberOfReplica()))
                            .flatMap(replicaServer -> keyNetworkPort.saveValue(replicaServer, dataObject, true)
                                    .zipWith(Mono.just(replicaServer)))
                            .take(quorumProperty.getWrite())
                            .doOnNext(tuple2 -> log.info("[Key] Ack of putting data came from {} ", tuple2.getT2().getName()))
                            .then(Mono.just(true))
                    );
        }

        return keyNetworkPort.saveValue(server, dataObject, false);
    }

    @Override
    public Mono<Boolean> saveReplicaData(DataObject dataObject) {
        HashKey key = HashKey.builder().key(dataObject.getKey()).build();
        List<HashServer> replicaServers = hashServicePort.getReplicaServers(key, quorumProperty.getNumberOfReplica());
        if(!replicaServers.contains(myHashServer)) {
            return Mono.just(false);
        }
        return mongoRepository.saveValue(key, dataObject, true)
                .then(Mono.just(true));
    }
}
