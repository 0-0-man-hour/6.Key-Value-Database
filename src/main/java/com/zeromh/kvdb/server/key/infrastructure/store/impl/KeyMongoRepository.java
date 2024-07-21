package com.zeromh.kvdb.server.key.infrastructure.store.impl;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.key.infrastructure.store.KeyStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
@RequiredArgsConstructor
public class KeyMongoRepository implements KeyStorePort {
    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private static final String COLLECTION = "save";

    @Override
    public Mono<DataObject> getValue(HashKey key) {
        return reactiveMongoTemplate.findById(key.getKey(), DataObject.class, COLLECTION);
    }

    @Override
    public Mono<DataObject> saveValue(HashKey key, DataObject dataObject) {
        Query query = new Query(Criteria.where("_id").is(dataObject.getKey()));
        Update update = new Update()
                .set("value", dataObject.getValue())
                .set("vectorClock", dataObject.getVectorClock());
        return reactiveMongoTemplate.upsert(query, update, COLLECTION)
                .thenReturn(dataObject);
    }

    @Override
    public Flux<DataObject> getAllValue() {
        return reactiveMongoTemplate.findAll(DataObject.class, COLLECTION);
    }
}
