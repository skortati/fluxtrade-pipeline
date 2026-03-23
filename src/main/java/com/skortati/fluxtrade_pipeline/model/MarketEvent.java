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
        String headline,
        double sentimentScore, // to be filled by the SentimentPlugin
        double volatilityScore,
        boolean isAlertTriggered, // To be set by the RiskConfidencePlugin
        AlertType alertType
) {
    public MarketEvent withSentiment(double score) {
        return new MarketEvent(symbol, price, timestamp, volume, headline, score, volatilityScore, isAlertTriggered, alertType);
    }

    public MarketEvent withAlert(boolean alert, AlertType type) {
        return new MarketEvent(symbol, price, timestamp, volume, headline, sentimentScore, volatilityScore, alert, type);
    }

    public MarketEvent withVolatility(double volatility) {
        return new MarketEvent(symbol, price, timestamp, volume, headline, sentimentScore, volatility, isAlertTriggered, alertType);
    }

    public MarketEvent withHeadline(String headline) {
        return new MarketEvent(symbol, price, timestamp, volume, headline, sentimentScore, volatilityScore, isAlertTriggered, alertType);
    }

    // Canonical constructor for easy transformation from TradeTick
    public static MarketEvent fromTick(TradeTick tick) {
        return new MarketEvent(
                tick.symbol(),
                tick.price(),
                Instant.ofEpochMilli((tick.timestamp())),
                tick.volume(),
                "",
                0.0,
                0.0,
                false,
                AlertType.NONE
        );
    }

}
