package com.zeromh.kvdb.server.gossip.dto;

import com.zeromh.kvdb.server.common.domain.Membership;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GossipUpdateDto {
    String requestServer;
    List<Membership> memberships;

    public GossipUpdateDto(String requestServer, List<Membership> memberships) {
        this.requestServer = requestServer;
        this.memberships = memberships;
    }
}
