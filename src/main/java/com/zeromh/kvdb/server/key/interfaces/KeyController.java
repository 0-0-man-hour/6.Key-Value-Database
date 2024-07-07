package com.zeromh.kvdb.server.key.interfaces;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.kvdb.server.key.application.KeyUseCase;
import com.zeromh.kvdb.server.common.domain.DataObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/key")
@RequiredArgsConstructor
public class KeyController {
    private final KeyUseCase keyUseCase;

    @GetMapping({"/{key}"})
    public Mono<DataObject> get(@PathVariable String key) {
        log.info("[Key] Request: get key: {}", key);
        return keyUseCase.getData(HashKey.builder().key(key).build());
    }

    @GetMapping("/replica/{key}")
    public Mono<DataObject> getReplica(@PathVariable String key) {
        log.info("[Key] Request: get replicated key: {}", key);
        return keyUseCase.getReplicaData(HashKey.builder().key(key).build());
    }

    @PostMapping
    public Mono<Boolean> put(@RequestBody DataObject dataObject) {
        log.info("[Key] Request: put key|value: {}|{}}", dataObject.getKey(), dataObject.getValue());
        return keyUseCase.saveData(dataObject);
    }

    @PostMapping("/replica")
    public Mono<Boolean> putReplica(@RequestBody DataObject dataObject) {
        log.info("[Key] Request: put replicated key|value: {}|{}}", dataObject.getKey(), dataObject.getValue());
        return keyUseCase.saveReplicaData(dataObject);
    }


}
