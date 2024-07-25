package com.zeromh.kvdb.server.merkle.interfaces;

import com.zeromh.kvdb.server.merkle.application.MerkleUseCase;
import com.zeromh.kvdb.server.merkle.domain.dto.MerkleRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/merkle")
@RequiredArgsConstructor
public class MerkleController {

    private final MerkleUseCase merkleUseCase;

    @GetMapping
    public Mono<Long> getMerkleBucketHashValue(@ModelAttribute MerkleRequestDto merkleRequestDto) {
        return merkleUseCase.getHashValue(merkleRequestDto);
    }

    @PostMapping public Flux<String> postMerkleBucketHashValue(@RequestBody MerkleRequestDto merkleRequestDto) {
        return merkleUseCase.compareMerkleAndFindDifferentKeys(merkleRequestDto);
    }
}
