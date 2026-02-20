package com.skortati.fluxtrade_pipeline.pipeline;

import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

/**
 * The base interface for all transformation steps.
 * Makes it an extensible plugin framework.
 */
public interface TradePlugin extends Ordered {
    /**
     * Processes the event. Returns a Mono to maintain non-blocking flow.
     */
    Mono<MarketEvent> process(MarketEvent event);

    /**
     * Used for runtime reconfiguration.
     */
    boolean isEnabled();
}
