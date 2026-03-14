package com.skortati.fluxtrade_pipeline.core;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance, lock-free cache for the latest market context.
 * This acts as the 'Side-Car' state for the reactive pipeline.
 */
@Component
public class NewsCache {
    // Key: Symbol (e.g., NVDA), Value: Latest Headline
    private final ConcurrentHashMap<String, String> latestHeadlines = new ConcurrentHashMap<>();

    // Optional: Cache the sentiment to avoid re-running NLP on the same headline
    private final ConcurrentHashMap<String, Double> sentimentScores = new ConcurrentHashMap<>();

    public void update(String symbol, String headline) {
        latestHeadlines.put(symbol, headline);
    }

    public void updateSentiment(String symbol, double score) {
        sentimentScores.put(symbol, score);
    }

    public String getFor(String symbol) {
        return latestHeadlines.getOrDefault(symbol, "No recent news");
    }

    public double getSentimentFor(String symbol) {
        return sentimentScores.getOrDefault(symbol, 0.0);
    }
}
