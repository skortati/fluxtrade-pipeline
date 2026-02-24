package com.skortati.fluxtrade_pipeline.plugins;

import com.skortati.fluxtrade_pipeline.core.TradePlugin;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(2)
public class VolatilityPlugin implements TradePlugin {
    // store a small history of prices to calculate the swing
    private final List<Double> priceHistory = new ArrayList<>();
    private static final int WINDOW_SIZE = 20; // last 20 ticks

    @Override
    public Mono<MarketEvent> process(MarketEvent event) {
        return Mono.fromCallable(() -> {
            synchronized (priceHistory) {
                // add current price to history
                priceHistory.add(event.price());

                // keep the window sliding (remove oldest)
                if (priceHistory.size() > WINDOW_SIZE) {
                    priceHistory.removeFirst();
                }

                // calculate volatilityScore (standard deviation)
                double volatility = calculateStatndardDeviation(priceHistory);

                return event.withVolatility(volatility);
            }
        });
    }



    private double calculateStatndardDeviation(List<Double> prices) {
        if (prices.size() < 2) {
            return 0.0;
        }

        double mean = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = prices.stream()
                .mapToDouble(p -> Math.pow(p - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }
}
