package com.skortati.fluxtrade_pipeline.plugins;

import com.skortati.fluxtrade_pipeline.core.TradePlugin;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
@Order(2)
public class VolatilityPlugin implements TradePlugin {
    // Key: Symbol (NVDA, BTC), Value: The sliding window for that symbol
    private final ConcurrentHashMap<String, Deque<Double>> priceHistories = new ConcurrentHashMap<>();
    private static final int WINDOW_SIZE = 20; // last 20 ticks

    @Override
    public Mono<MarketEvent> process(MarketEvent event) {
        return Mono.fromCallable(() -> {
            // ConcurrentLinkedDeque is non-blocking and perfect for a "Sliding Window"
           Deque<Double> history = priceHistories.computeIfAbsent(event.symbol(), s -> new ConcurrentLinkedDeque<>());
           history.add(event.price());
           if (history.size() > WINDOW_SIZE) {
               history.pollFirst();
           }
           double volatility = calculateStandardDeviation(new ArrayList<>(history));
           return event.withVolatility(volatility);
        }).subscribeOn(Schedulers.parallel()); // Standard deviation is CPU-bound
    }



    private double calculateStandardDeviation(List<Double> prices) {
        if (prices.size() < 2) return 0.0;
        double mean = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = prices.stream()
                .mapToDouble(p -> Math.pow(p - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }
}
