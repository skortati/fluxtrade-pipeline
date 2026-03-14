package com.skortati.fluxtrade_pipeline.core;

import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineEngine.class);
    // Spring automatically injects all beans implementing TradePlugin, sorted by @Order
    private final List<TradePlugin> plugins;

    public Mono<MarketEvent> runPipeline(MarketEvent initialEvent) {
        Mono<MarketEvent> pipeline = Mono.just(initialEvent);

        for (TradePlugin plugin : plugins) {
            pipeline = pipeline.flatMap(plugin::process);
        }

        return pipeline
                .doOnError(e -> LOGGER.error("Pipeline failed", e))
                .doOnNext(event -> LOGGER.info("Pipeline complete for {}", event.symbol()));
    }
}
