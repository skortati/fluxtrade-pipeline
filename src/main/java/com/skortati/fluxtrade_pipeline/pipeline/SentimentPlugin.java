package com.skortati.fluxtrade_pipeline.pipeline;

import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Getter
@Setter
public class SentimentPlugin implements TradePlugin {
    private boolean enabled = true; // default state

    @Override
    public Mono<MarketEvent> process(MarketEvent event) {
        double score = 0.0;
//        String headline = event.vo().toLowerCase();
//
//        // Simple heuristic: "Bullish" words increase score, "Bearish" decrease it
//        if (headline.contains("breakthrough") || headline.contains("growth")) score += 0.8;
//        if (headline.contains("lawsuit") || headline.contains("shortage")) score -= 0.8;

        return Mono.just(event.withSentiment(score));
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getOrder() {
        return 1; // Runs first to provide data for later plugins
    }
}
