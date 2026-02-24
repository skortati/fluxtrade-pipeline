package com.skortati.fluxtrade_pipeline.core;

import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

/**
 * The base interface for all transformation steps.
 * Makes it an extensible plugin framework.
 */
@FunctionalInterface
public interface TradePlugin {
    /**
     * Processes the event. Returns a Mono to maintain non-blocking flow.
     */
    Mono<MarketEvent> process(MarketEvent event);
}
