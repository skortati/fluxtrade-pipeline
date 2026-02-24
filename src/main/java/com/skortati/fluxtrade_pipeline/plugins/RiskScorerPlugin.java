package com.skortati.fluxtrade_pipeline.plugins;

import com.skortati.fluxtrade_pipeline.core.TradePlugin;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Order(3)
public class RiskScorerPlugin implements TradePlugin {
    // Thresholds: would usually come from application.yaml
    private static final double SENTIMENT_PANIC_THRESHOLD = -0.5; // "Negative" or "Very Negative"
    private static final double VOLATILITY_ALARM_THRESHOLD = 0.02; // 2% price swing

    @Override
    public Mono<MarketEvent> process(MarketEvent event) {
        return Mono.fromCallable(() -> {
            // the logic gate:
            // high risk = high volatility + bad sentiment
            boolean shouldTrigger = (event.sentimentScore() <= SENTIMENT_PANIC_THRESHOLD)
                    && (event.volatilityScore() <= VOLATILITY_ALARM_THRESHOLD);

            if (shouldTrigger) {
                System.out.println("🚨 [ALERT] High Risk Detected for " + event.symbol());
                System.out.println("Context: Sentiment " + event.sentimentScore() + " | Volatility " + event.volatilityScore());
            }

            return event.withAlert(shouldTrigger);
        });
    }
}
