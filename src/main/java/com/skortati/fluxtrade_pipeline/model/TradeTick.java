package com.skortati.fluxtrade_pipeline.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an individual trade event from the Finnhub WebSocket.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeTick(
        @JsonProperty("p") double price,
        @JsonProperty("s") String symbol,
        @JsonProperty("t") long timestamp,
        @JsonProperty("v") double volume
) {
}
