package com.zeromh.kvdb.server.key.application.impl;

import com.zeromh.consistenthash.application.dto.HashServerDto;
import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.consistenthash.domain.model.server.HashServer;
import com.zeromh.consistenthash.domain.service.hash.HashServicePort;
import com.zeromh.kvdb.server.common.ServerManager;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.common.domain.VectorClock;
import com.zeromh.kvdb.server.common.dto.MerkleHashDto;
import com.zeromh.kvdb.server.config.QuorumProperty;
import com.zeromh.kvdb.server.handoff.application.HandoffUseCase;
import com.zeromh.kvdb.server.key.application.KeyUseCase;
import com.zeromh.kvdb.server.key.infrastructure.network.KeyNetworkPort;
import com.zeromh.kvdb.server.key.infrastructure.store.KeyStorePort;
import com.zeromh.kvdb.server.merkle.application.MerkleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyService implements KeyUseCase {

    private final ServerManager serverManager;
    private final HashServicePort hashServicePort;
    private final QuorumProperty quorumProperty;

    private final HandoffUseCase handoffUseCase;
    private final MerkleUseCase merkleUseCase;

    private final KeyNetworkPort keyNetworkPort;
    private final KeyStorePort keyStorePort;

    @Override
    public Mono<DataObject> intermediateGetData(HashKey key) {
        List<HashServerDto> targetServers = hashServicePort.getAliveServers(key, quorumProperty.getNumberOfReplica());
        List<HashServer> responsibleServers = new ArrayList<>(targetServers.stream().filter(hashServerDto -> hashServerDto.getFailureServer() == null)
                .map(HashServerDto::getServer).toList());
        if (responsibleServers.contains(serverManager.getMyServer())) {
            return Flux.fromIterable(responsibleServers)
                    .flatMap(responsibleServer -> fetchDataFromServer(responsibleServer, key))
                    .take(quorumProperty.getRead())
                    .doOnNext(tuple2 -> log.info("[Key] Ack of putting {} came from {} ", key.getKey() ,tuple2.getT2().getName()))
                    .collectList()
                    .flatMap(tuple2 -> checkConflictAndSendLatestVersion(key, tuple2));
        }

        return intermediateGetDataToResponsibleServers(responsibleServers, key, 0);
    }

    private Mono<Tuple2<DataObject, HashServer>> fetchDataFromServer(HashServer server, HashKey key) {
        if (server.equals(serverManager.getMyServer())) {
            return getData(key)
                    .zipWith(Mono.just(server));
        }
        return keyNetworkPort.fetchKeyValue(server, key, true)
                .zipWith(Mono.just(server));
    }

    private Mono<DataObject> intermediateGetDataToResponsibleServers(List<HashServer> servers, HashKey key, int index) {
        if (index >= servers.size()) {
            return Mono.error(new RuntimeException("All servers failed"));
        }
        HashServer currentServer = servers.get(index);
        log.info("[Key] {} is selected as the coordinator server for GET", currentServer.getName());
        return keyNetworkPort.fetchKeyValue(currentServer, key, false)
                .onErrorResume(error -> intermediateGetDataToResponsibleServers(servers, key, index + 1));
    }

    @Override
    public Mono<DataObject> getData(HashKey key) {
        return keyStorePort.getValue(key);
    }

    private Mono<DataObject> checkConflictAndSendLatestVersion(HashKey key, List<Tuple2<DataObject, HashServer>> tupleData) {
        Tuple2<DataObject, HashServer> latest = tupleData.get(0);
        boolean isConflict = false;
        for (Tuple2<DataObject, HashServer> item : tupleData) {
            VectorClock.ComparisonResult result = latest.getT1().getVectorClock().compare(item.getT1().getVectorClock());
            if (result == VectorClock.ComparisonResult.BEFORE) {
                latest = item;
                isConflict = true;
            }
        }

        if (isConflict) {
            latest.getT1().getVectorClock().tick(serverManager.getMyServer().getName());
            log.info("[Key] resolve conflict data. update key: {}", latest.getT1().getKey());
            DataObject finalData = latest.getT1();
            finalData.setMerkleHashDto(MerkleHashDto.fromHashKey(key));
            return Flux.fromIterable(tupleData)
                    .flatMap(dataObject -> {
                        if (dataObject.getT2().equals(serverManager.getMyServer())) {
                            return saveData(key, finalData);
                        }
                        return keyNetworkPort.saveValue(dataObject.getT2(), finalData, true);
                    })
                    .then(Mono.just(latest.getT1()));
        }
        return Mono.just(latest.getT1());
    }

    @Override
    public Mono<Boolean> intermediateSaveData(DataObject dataObject) {
        HashKey key = HashKey.builder().key(dataObject.getKey()).build();
        List<HashServerDto> targetServers = hashServicePort.getAliveServers(key, quorumProperty.getNumberOfReplica());
        List<HashServer> responsibleServers = new ArrayList<>(targetServers.stream().filter(hashServerDto -> hashServerDto.getFailureServer() == null)
                .map(HashServerDto::getServer).toList());

        if (responsibleServers.contains(serverManager.getMyServer())) {
            dataObject.setMerkleHashDto(MerkleHashDto.fromHashKey(key));
            return getDataAndUpdateVectorClock(key, dataObject)
                    .thenMany(Flux.fromIterable(targetServers))
                    .flatMap(targetServer -> saveDataToServers(targetServer, key, dataObject).zipWith(Mono.just(targetServer.getServer())))
                    .take(quorumProperty.getWrite())
                    .doOnNext(tuple2 -> log.info("[Key] Ack of putting {} came from {} ", key.getKey() ,tuple2.getT2().getName()))
                    .then(Mono.just(true));
        }

        return intermediateSaveDataToResponsibleServers(responsibleServers, dataObject, 0);
    }

    private Mono<DataObject> getDataAndUpdateVectorClock(HashKey key, DataObject dataObject) {
        return keyStorePort.getValue(key)
                .defaultIfEmpty(dataObject)
                .map(save -> updateOrCreateVectorClock(save, dataObject));
    }

    private DataObject updateOrCreateVectorClock(DataObject save, DataObject request) {
        VectorClock saveVectorClock = save.getVectorClock();
        if (saveVectorClock == null) {
            VectorClock vectorClock = new VectorClock();
            vectorClock.tick(serverManager.getMyServer().getName());
            request.setVectorClock(vectorClock);
        } else {
            saveVectorClock.tick(serverManager.getMyServer().getName());
            request.setVectorClock(saveVectorClock);
        }

        return request;
    }

    private Mono<DataObject> saveDataToServers(HashServerDto serverDto, HashKey key, DataObject dataObject) {
        HashServer server = serverDto.getServer();
        if (serverDto.getFailureServer() != null) {
            return handoffUseCase.leaveData(server.getName(), serverDto.getFailureServer().getName(), dataObject)
                    .thenReturn(dataObject);
        }

        if (server.equals(serverManager.getMyServer())) {
            return saveData(key, dataObject);
        };

        return keyNetworkPort.saveValue(server, dataObject, true)
                .thenReturn(dataObject);
    }

    private Mono<Boolean> intermediateSaveDataToResponsibleServers(List<HashServer> servers, DataObject dataObject, int index) {
        if (index >= servers.size()) {
            return Mono.error(new RuntimeException("All servers failed to save the data"));
        }

        HashServer currentServer = servers.get(index);
        log.info("[Key] {} is selected as the coordinator server for SAVE", currentServer.getName());
        return keyNetworkPort.saveValue(currentServer, dataObject, false)
                .onErrorResume(error -> intermediateSaveDataToResponsibleServers(servers, dataObject, index + 1));
    }

    @Override
    public Mono<DataObject> saveData(HashKey key, DataObject request) {
        return keyStorePort.saveValue(key, request)
                .flatMap(merkleUseCase::updateMerkle)
                .thenReturn(request);
    }
}
