package com.zeromh.kvdb.server.common.domain;

import com.zeromh.kvdb.server.common.dto.MerkleHashDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class DataObject {
    @Id
    String key;
    Object value;
    @Setter
    VectorClock vectorClock;
    @Setter
    String serverName;
    @Setter
    MerkleHashDto merkleHashDto;

}
