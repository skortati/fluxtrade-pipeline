package com.skortati.fluxtrade_pipeline.model;

public enum AlertType {
    NONE,
    BLACK_SWAN, // high volatility + negative sentiment
    BULL_RALLY // high volatility + positiven sentiment
}
