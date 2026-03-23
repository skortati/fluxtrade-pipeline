package com.skortati.fluxtrade_pipeline.plugins;

import com.skortati.fluxtrade_pipeline.core.TradePlugin;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import com.vader.sentiment.analyzer.SentimentAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Order(1)
public class SentimentPlugin implements TradePlugin {
    private final static Logger LOGGER = LoggerFactory.getLogger(SentimentPlugin.class);

    @Override
    public Mono<MarketEvent> process(MarketEvent event) {
        if (event.headline() == null || event.headline().isBlank()) {
            return Mono.just(event);
        }

        return Mono.fromCallable(() -> {
            try {
                // VADER: Lexicon-based sentiment analysis (-1.0 to 1.0)
                var polarities = SentimentAnalyzer.getScoresFor(event.headline());
                float compoundScore = polarities.getCompoundPolarity();

                return event.withSentiment(compoundScore);
            } catch (Exception e) {
                LOGGER.error("[SentimentPlugin] Error parsing headline: {}", e.getMessage());
                return event;
            }
        }).subscribeOn(Schedulers.parallel()); // Lexicon-based is light enough for parallel()
    }

}
