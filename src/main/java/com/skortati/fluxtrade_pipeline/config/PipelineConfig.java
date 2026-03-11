package com.skortati.fluxtrade_pipeline.config;

import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Sinks;

@Configuration
public class PipelineConfig {
    /**
     * multicast(): Allows multiple subscribers (Plugins) to listen to the same stream.
     * onBackpressureBuffer(1024): If the SentimentPlugin (NLP) gets slow,
     * we buffer up to 1024 events before we start dropping them.
     * This is key for NVIDIA-grade stability.
     */
    @Bean
    public Sinks.Many<MarketEvent> pipelineSink() {
        return Sinks.many()
                .multicast()
                .onBackpressureBuffer(1024);
    }

    @Bean
    public WebClient finnhubWebClient() {
        return WebClient.builder().baseUrl("https://finnhub.io/api/v1").build();
    }
}
