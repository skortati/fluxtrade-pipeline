package com.skortati.fluxtrade_pipeline.plugins;

import com.skortati.fluxtrade_pipeline.core.TradePlugin;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import com.skortati.fluxtrade_pipeline.model.RollingStats;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(2)
public class VolatilityPlugin implements TradePlugin {
    // Key: Symbol (NVDA, BTC), Value: The sliding window for that symbol
    // we use a primitive-friendly approach to avoid Garbage Collector pressure
    private final ConcurrentHashMap<String, RollingStats> statsMap = new ConcurrentHashMap<>();
    private static final int WINDOW_SIZE = 20; // last 20 ticks

    @Override
    public Mono<MarketEvent> process(MarketEvent event) {
        return Mono.fromCallable(() -> {
           var stats = statsMap.computeIfAbsent(event.symbol(), s -> new RollingStats(20));

           // Circular buffer logic: no new list allocations!
            int idx = stats.index().getAndIncrement() % WINDOW_SIZE;
            stats.window()[idx] = event.price();
            stats.count().incrementAndGet();

            return event.withVolatility(calculateStdDev(stats));
        }).subscribeOn(Schedulers.parallel()); // Standard deviation is CPU-bound
    }

    private double calculateStdDev(RollingStats stats) {
        int limit = Math.min(stats.count().get(), 20);
        if (limit < 2) return 0.0;

        double sum = 0;
        for (int i = 0; i < limit; i++) sum += stats.window()[i];
        double mean = sum / limit;

        double sumSqDiff = 0;
        for (int i = 0; i < limit; i++) {
            sumSqDiff += Math.pow(stats.window()[i] - mean, 2);
        }
        return Math.sqrt(sumSqDiff / limit);
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
