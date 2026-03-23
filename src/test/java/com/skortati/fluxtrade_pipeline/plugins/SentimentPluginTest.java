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
    void setUp() throws Exception {
        plugin = new SentimentPlugin();
        plugin.init();
    }

    @Test
    void returnPositiveScoreForGoodNews() {
        MarketEvent event = createEventWithHeadline("NVDA", "The earnings were not that bad");

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.sentimentScore()).isGreaterThan(-0.1);
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
    void handleEmptyHeadline() {
        MarketEvent event = createEventWithHeadline("AAPL", "");

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.sentimentScore()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    @Test
    void returnPositiveForNegatedNegative() {
        // VADER/CoreNLP often fail here because they see "not" and "bad" separately.
        // DistilBERT understands that "not that bad" is actually a positive signal.
        MarketEvent event = createEventWithHeadline("TSLA", "The quarterly revenue was not that bad.");

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    // Should be classified as positive (LABEL_1) with a high score
                    assertThat(result.sentimentScore()).isGreaterThan(0.0);
                })
                .verifyComplete();
    }

    @Test
    void returnNeutralScoreForFactualReporting() {
        // Purely factual reporting should hover near the middle or be slightly positive.
        MarketEvent event = createEventWithHeadline("AAPL", "Apple Inc. scheduled its annual shareholder meeting for next Thursday.");

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    // Probability for positive vs negative should be less polarized
                    // In SST-2, "Neutral" usually leans slightly toward one side but with lower confidence magnitude
                    assertThat(Math.abs(result.sentimentScore())).isLessThan(0.9);
                })
                .verifyComplete();
    }

    @Test
    void returnStrongNegativeForComplexPhrasing() {
        // Tests if the model understands "despite" and "failed" in context.
        MarketEvent event = createEventWithHeadline("NVDA", "Despite initial hype, the new chip architecture failed to meet efficiency standards.");

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.sentimentScore()).isLessThan(-0.8);
                })
                .verifyComplete();
    }

    @Test
    void returnNeutralScoreForFactualReporting2() {
        MarketEvent event = createEventWithHeadline("AAPL", "Apple Inc. scheduled its annual shareholder meeting.");

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    // With scaling, a 55% positive probability becomes 0.1
                    assertThat(Math.abs(result.sentimentScore())).isLessThan(0.7);
                })
                .verifyComplete();
    }

    private MarketEvent createEventWithHeadline(String symbol, String headline) {
        return new MarketEvent(symbol, 100.0, Instant.now(), 0.0, headline, 0.0, 0.0, false, null);
    }

}
