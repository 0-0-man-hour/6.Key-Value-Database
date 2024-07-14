package com.zeromh.kvdb.server.handoff.infrastructure.store.impl;

import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.handoff.infrastructure.store.HandoffStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class HandoffMongoRepository implements HandoffStorePort {

    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private static final String COLLECTION = "save";

    @Override
    public Flux<DataObject> getAllValueAndRemove(String serverName) {
        Query query = new Query(Criteria.where("serverName").is(serverName));
        return reactiveMongoTemplate.findAllAndRemove(query, DataObject.class, COLLECTION);
    }

    @Override
    public Mono<DataObject> saveValue(String serverName, DataObject dataObject) {
        Query query = new Query(Criteria.where("_id").is(dataObject.getKey()));
        Update update = new Update()
                .set("value", dataObject.getValue())
                .set("vectorClock", dataObject.getVectorClock())
                .set("serverName", serverName);
        return reactiveMongoTemplate.upsert(query, update, COLLECTION)
                .thenReturn(dataObject);
    }
}
