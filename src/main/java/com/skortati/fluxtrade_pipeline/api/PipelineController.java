package com.skortati.fluxtrade_pipeline.api;

import com.skortati.fluxtrade_pipeline.core.PipelineEngine;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineController {
    private final PipelineEngine pipelineEngine;

    @PostMapping("/process")
    public Mono<MarketEvent> processEvent(@RequestBody MarketEvent request) {
        return pipelineEngine.runPipeline(request);
    }

}
