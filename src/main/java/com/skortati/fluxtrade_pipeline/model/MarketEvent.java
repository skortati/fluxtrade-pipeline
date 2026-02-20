package com.skortati.fluxtrade_pipeline.model;

import java.time.Instant;

/**
 * Represents a single tick of market data.
 * Records are used to ensure immutability in our reactive stream.
 */
public record MarketEvent(
        String symbol,
        double price,
        Instant timestamp,
        double volume,
        double sentimentScore, // to be filled by the SentimentPlugin
        boolean isAlertTriggered // To be set by the RiskConfidencePlugin
) {
    public MarketEvent withSentiment(double score) {
        return new MarketEvent(symbol, price, timestamp, volume, sentimentScore, isAlertTriggered);
    }

    public MarketEvent withAlert(boolean alert) {
        return new MarketEvent(symbol, price, timestamp, volume, sentimentScore, alert);
    }

    // Canonical constructor for easy transformation from TradeTick
    public static MarketEvent fromTick(TradeTick tick) {
        return new MarketEvent(
                tick.symbol(),
                tick.price(),
                Instant.ofEpochMilli((tick.timestamp())),
                tick.volume(),
                0.0,
                false
        );
    }

}
