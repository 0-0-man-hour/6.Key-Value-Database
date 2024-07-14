package com.zeromh.kvdb.server.common.dto;

import com.zeromh.kvdb.server.common.domain.DataObject;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HandoffDto {
    private String serverName;
    private DataObject dataObject;
}
