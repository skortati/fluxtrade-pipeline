package com.skortati.fluxtrade_pipeline.model;

public record NewsRecord(
        String category,
        long datetime,
        String headline,
        int id,
        String image,
        String related, // The Ticker Symbol
        String source,
        String summary,
        String url
) {
}
