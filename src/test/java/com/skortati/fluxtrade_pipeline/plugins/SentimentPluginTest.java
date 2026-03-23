package com.skortati.fluxtrade_pipeline.plugins;

import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SentimentPluginTest {
    private SentimentPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = new SentimentPlugin();
    }

    @Test
    void returnPositiveScoreForGoodNews() {
        MarketEvent event = createEventWithHeadline("NVDA", "NVIDIA reports record-breaking quarterly profits");

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.sentimentScore()).isGreaterThan(0.0);
                })
                .verifyComplete();
    }

    @Test
    void returnNegativeScoreForPanicNews() {
        MarketEvent event = createEventWithHeadline("BTC", "Bitcoin price crashes in a catastrophic sell-off after regulatory ban.");

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.sentimentScore()).isLessThan(0.0);
                })
                .verifyComplete();
    }

    @Test
    void testNeutralSentiment() {
        MarketEvent event = createEventWithHeadline("AAPL ", "AAPL releases new colors.");

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.sentimentScore()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    @Test
    void handleEmptyHeadline() {
        MarketEvent event = createEventWithHeadline("AAPL", "");

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.sentimentScore()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    private MarketEvent createEventWithHeadline(String symbol, String headline) {
        return new MarketEvent(symbol, 100.0, Instant.now(), 0.0, headline, 0.0, 0.0, false);
    }

}
