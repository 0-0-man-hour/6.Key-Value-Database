package com.zeromh.kvdb.server.application.impl;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.consistenthash.domain.service.hash.HashServicePort;
import com.zeromh.kvdb.server.application.KeyUseCase;
import com.zeromh.kvdb.server.infrastructure.network.NetworkPort;
import com.zeromh.kvdb.server.config.QuorumProperty;
import com.zeromh.kvdb.server.domain.DataObject;
import com.zeromh.kvdb.server.infrastructure.store.StorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyService implements KeyUseCase {

    private final HashServer myHashServer;
    private final HashServicePort hashServicePort;
    private final QuorumProperty quorumProperty;

    private final NetworkPort networkPort;
    private final StorePort storePort;


    @Override
    public Mono<DataObject> getData(HashKey key) {
        HashServer server = hashServicePort.getServer(key);
        if (server.equals(myHashServer)) {
            log.info("myserver({}) is target of getting data(key={}).", server.getName(), key.getKey());
            return storePort.getValue(key, false);

        }
        return networkPort.fetchKeyValue(server, key, false);

    }

    @Override
    public Mono<DataObject> getReplicaData(HashKey key) {
        return storePort.getValue(key, true);
    }

    @Override
    public Mono<Boolean> saveData(DataObject dataObject) {
        HashKey key = HashKey.builder().key(dataObject.getKey()).build();
        HashServer server = hashServicePort.getServer(key);
        if (server.equals(myHashServer)) {
            log.info("myserver({}) is target of putting data(key={}).", server.getName(), dataObject.getKey());
            return storePort.saveValue(key, dataObject, false).thenReturn(true);
        }

        return networkPort.saveValue(server, dataObject, false);
    }

    @Override
    public Mono<Boolean> saveReplicaData(DataObject dataObject) {
        HashKey key = HashKey.builder().key(dataObject.getKey()).build();
        List<HashServer> replicaServers = hashServicePort.getReplicaServers(key, quorumProperty.getNumberOfReplica());
        if(!replicaServers.contains(myHashServer)) {
            return Mono.just(false);
        }
        return storePort.saveValue(key, dataObject, true)
                .then(Mono.just(true));
    }
}
