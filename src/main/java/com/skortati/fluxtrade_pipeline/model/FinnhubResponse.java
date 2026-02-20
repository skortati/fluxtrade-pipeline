package com.skortati.fluxtrade_pipeline.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the top-level WebSocket message from Finnhub.
 */
public record FinnhubResponse(
        @JsonProperty("type") String type,
        @JsonProperty("data")List<TradeTick> data
        )
{}
