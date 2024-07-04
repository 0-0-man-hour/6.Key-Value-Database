package com.zeromh.kvdb.server.common.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class DataObject {
    @Id
    String key;
    Object value;
}
