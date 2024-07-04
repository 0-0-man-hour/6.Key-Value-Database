package com.zeromh.kvdb.server.storage.infrastructure.store.impl;

import com.zeromh.consistenthash.domain.model.key.HashKey;
import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.storage.infrastructure.store.StorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;


@Repository
@RequiredArgsConstructor
public class MongoRepository implements StorePort {
    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private static final String REPLICA = "R_";

    @Override
    public Mono<DataObject> getValue(HashKey key, boolean isReplica) {
        String collectionName = (isReplica?REPLICA:"")+key.getServerHash();
        return reactiveMongoTemplate.findById(key.getKey(), DataObject.class, collectionName);
    }

    @Override
    public Mono<DataObject> saveValue(HashKey key, DataObject dataObject, boolean isReplica) {
        String collectionName = (isReplica?REPLICA:"")+key.getServerHash();
        Query query = new Query(Criteria.where("_id").is(dataObject.getKey()));
        Update update = new Update().set("value", dataObject.getValue());
        return reactiveMongoTemplate.upsert(query, update, collectionName)
                .thenReturn(dataObject);
    }
}
