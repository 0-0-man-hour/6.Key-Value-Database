package com.zeromh.kvdb.server.handoff.interfaces;

import com.zeromh.kvdb.server.common.domain.DataObject;
import com.zeromh.kvdb.server.common.dto.HandoffDto;
import com.zeromh.kvdb.server.handoff.application.HandoffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/handoff")
@RequiredArgsConstructor
public class HandoffController {

    private final HandoffService handoffService;

    @PostMapping
    public Mono<Boolean> postLeaveData(@RequestBody HandoffDto handoffDto) {
        log.info("[Handoff] Keep data(key: {}) of {}", handoffDto.getDataObject().getKey(), handoffDto.getServerName());
        return handoffService.keepData(handoffDto.getServerName(), handoffDto.getDataObject());
    }

    @GetMapping
    public Flux<DataObject> getAllLeftData(@RequestParam String serverName) {
        log.info("[Handoff] Send all left data of {}", serverName);
        return handoffService.getAllLeftData(serverName);
    }
}
