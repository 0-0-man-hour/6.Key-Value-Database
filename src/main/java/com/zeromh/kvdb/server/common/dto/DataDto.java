package com.zeromh.kvdb.server.common.dto;

import com.zeromh.kvdb.server.common.domain.DataObject;
import lombok.Builder;

@Builder
public class DataDto {
    String key;
    Object value;


    public DataDto fromDataObject(DataObject dataObject) {
        return DataDto.builder()
                .key(dataObject.getKey())
                .value(dataObject.getValue())
                .build();
    }
}
