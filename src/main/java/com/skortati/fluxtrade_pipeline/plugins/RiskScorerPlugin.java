package com.skortati.fluxtrade_pipeline.plugins;

import com.skortati.fluxtrade_pipeline.core.TradePlugin;
import com.skortati.fluxtrade_pipeline.model.AlertType;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Order(3)
public class RiskScorerPlugin implements TradePlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(RiskScorerPlugin.class);
    // Thresholds: would usually come from application.yaml
    private static final double SENTIMENT_PANIC_THRESHOLD = 0.5;
    private static final double VOLATILITY_ALARM_THRESHOLD = 0.02; // 2% price swing

    @Override
    public Mono<MarketEvent> process(MarketEvent event) {
        return Mono.fromCallable(() -> {
            boolean highVolatility = event.volatilityScore() >= VOLATILITY_ALARM_THRESHOLD;

            if (highVolatility) {
                if (event.sentimentScore() <= -SENTIMENT_PANIC_THRESHOLD) {
                    LOGGER.info("[BLACK SWAN] {} | Vol: {} | Sent: {}", event.symbol(), event.volatilityScore(), event.sentimentScore());
                    return event.withAlert(true, AlertType.BLACK_SWAN);
                }

                if (event.sentimentScore() >= SENTIMENT_PANIC_THRESHOLD) {
                    LOGGER.info("[BULL_RALLY] {} | Vol: {} | Sent: {}", event.symbol(), event.volatilityScore(), event.sentimentScore());
                    return event.withAlert(true, AlertType.BULL_RALLY);
                }
            }

            return event.withAlert(false, AlertType.NONE);
        });
    }
}
