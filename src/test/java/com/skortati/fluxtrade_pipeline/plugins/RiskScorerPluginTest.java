package com.skortati.fluxtrade_pipeline.plugins;

import com.skortati.fluxtrade_pipeline.model.AlertType;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class RiskScorerPluginTest {
    private RiskScorerPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = new RiskScorerPlugin();
    }

    @Test
    void shouldTriggerAlertOnHighVolatilityAndPanic() {
        // GIVEN: Sentiment is -0.8 (Panic) and Volatility is 0.05 (High)
        MarketEvent event = createEvent(-0.8, 0.05);

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.isAlertTriggered()).isTrue();
                    assertThat(result.alertType()).isEqualTo(AlertType.BLACK_SWAN);
                })
                .verifyComplete();
    }

    @Test
    void shouldNotTriggerAlertOnHighVolatilityButPositiveSentiment() {
        // GIVEN: Market is wild (0.05) and news is GREAT (0.9)
        MarketEvent event = createEvent(0.9, 0.05);

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.isAlertTriggered()).isTrue();
                    assertThat(result.alertType()).isEqualTo(AlertType.BULL_RALLY);
                })
                .verifyComplete();
    }

    @Test
    void shouldNotTriggerOnPanicButLowVolatility() {
        // GIVEN: Bad news (-0.9) but price hasn't moved yet (0.001)
        MarketEvent event = createEvent(-0.9, 0.001);

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.isAlertTriggered()).isFalse();
                    assertThat(result.alertType()).isEqualTo(AlertType.NONE);
                })
                .verifyComplete();
    }

    @Test
    void shouldNotTriggerOnHighVolatilityButNeutralSentiment() {
        // GIVEN: High Volatility (0.05) but noise/neutral news (0.1)
        MarketEvent event = createEvent(0.1, 0.05);

        StepVerifier.create(plugin.process(event))
                .assertNext(result -> {
                    assertThat(result.isAlertTriggered()).isFalse();
                    assertThat(result.alertType()).isEqualTo(AlertType.NONE);
                })
                .verifyComplete();
    }


    private MarketEvent createEvent(double sentiment, double volatility) {
        return new MarketEvent(
                "NVDA", 100.0, Instant.now(), 1.0,
                "Test Headline", sentiment, volatility, false, AlertType.NONE
        );
    }
}
