package com.skortati.fluxtrade_pipeline.pipeline;

import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineEngine {
    // Spring automatically injects all beans implementing TradePlugin, sorted by @Order
    private final List<TradePlugin> plugins;

    public Mono<MarketEvent> runPipeline(MarketEvent initialEvent) {
        Flux<MarketEvent> pipelineFlow = Flux.just(initialEvent);

        for (TradePlugin plugin : plugins) {
            pipelineFlow = pipelineFlow.flatMap(event -> {
                if (plugin.isEnabled()) {
                    return plugin.process(event);
                }
                return Mono.just(event);
            });
        }

        return pipelineFlow.single();
    }
}
