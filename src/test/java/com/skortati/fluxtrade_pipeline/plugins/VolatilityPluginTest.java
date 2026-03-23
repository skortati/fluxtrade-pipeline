package com.skortati.fluxtrade_pipeline.plugins;

import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class VolatilityPluginTest {
    private VolatilityPlugin plugin;

    @BeforeEach
    void setUp() {
        // reset the internal RollingStats map
        plugin = new VolatilityPlugin();
    }

    @Test
    void testInitialEvent() {
        MarketEvent firstEvent = createEvent("NVDA", 100.0);

        Mono<MarketEvent> result = plugin.process(firstEvent);

        StepVerifier.create(result)
                .assertNext(event -> {
                    assertThat(event.symbol()).isEqualTo("NVDA");
                    assertThat(event.volatilityScore()).isEqualTo(0.0);
                    assertThat(event.isAlertTriggered()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void testVolatilityCalculation() {
        MarketEvent e1 = createEvent("BTC", 50000.0);
        MarketEvent e2 = createEvent("BTC", 51000.0);
        MarketEvent e3= createEvent("BTC", 49000.0);

        // Process first two silently
        plugin.process(e1).block();
        plugin.process(e2).block();

        // Verify the third one
        StepVerifier.create(plugin.process(e3))
                .assertNext(event -> {
                    assertThat(event.volatilityScore()).isGreaterThan(0.0);
                    // Standard Deviation of {50k, 51k, 49k} is 816.49658092773
                    assertThat(event.volatilityScore()).isCloseTo(816.49, offset(0.01));
                })
                .verifyComplete();
    }

    private MarketEvent createEvent(String symbol, double price) {
        return new MarketEvent(
                symbol,
                price,
                Instant.now(),
                0.0,
                "",
                0.0,
                0.0,
                false,
                null
        );
    }
}
