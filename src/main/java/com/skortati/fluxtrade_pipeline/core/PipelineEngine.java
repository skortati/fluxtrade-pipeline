package com.skortati.fluxtrade_pipeline.core;

import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineEngine.class);
    // Spring automatically injects all beans implementing TradePlugin, sorted by @Order
    private final List<TradePlugin> plugins;

    public Mono<MarketEvent> runPipeline(MarketEvent initialEvent) {
        return Flux.fromIterable(plugins)
                .reduce(Mono.just(initialEvent), (eventMono, plugin) ->
                        eventMono.flatMap(event -> {
                            LOGGER.debug("Applying plugin: {} to {}",
                                    plugin.getClass().getSimpleName(), event.symbol());
                            return plugin.process(event);
                        })
                )
                .flatMap(mono -> mono); // Flatten the resulting Mono<Mono<MarketEvent>>
    }
}
