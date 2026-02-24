package com.skortati.fluxtrade_pipeline.ingestion;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;

@Service
public class MockNewsService {
    private final Random random = new Random();

    private final Map<String, String> priceHeadLines = Map.of(
            "NVDA", "NVIDIA quarterly earnings miss expectations; chip demand slowing down.",
            "BINANCE:BTCUSDT", "Major crypto exchange announces halt on all Bitcoin withdrawals."
    );

    public String getHeadlineFor(String symbol) {
        // 20% chance to return a panic headline to trigger the alert
        if (random.nextDouble() < 0.20) {
            return priceHeadLines.getOrDefault(symbol, "General market volatility observed.");
        }
        return "Market trading volume remains within normal parameters.";
    }
}
