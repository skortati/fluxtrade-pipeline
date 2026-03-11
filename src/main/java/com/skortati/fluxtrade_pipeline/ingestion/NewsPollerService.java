package com.skortati.fluxtrade_pipeline.ingestion;

import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import com.skortati.fluxtrade_pipeline.model.NewsRecord;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsPollerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsPollerService.class);
    private final WebClient webClient;
    private final Sinks.Many<MarketEvent> pipelineSink;
    private final String apiKey = "...";

    private final List<String> trackedSymbols = List.of("NVDA", "BTC", "ETH", "AAPL");

    @Scheduled(fixedRate = 60000) // every minute
    public void runPollingCycle() {
        Flux.fromIterable(trackedSymbols)
                .flatMap(this::fetchNewsForSymbol)
                .subscribe(
                        pipelineSink::tryEmitNext,
                        error -> LOGGER.error("Polling Error: {}", error.getMessage())
                );
    }

    private Flux<MarketEvent> fetchNewsForSymbol(String symbol) {
        boolean isCrypto = isCrypto(symbol);

        return webClient.get()
                .uri(uriBuilder -> {
                    if (isCrypto) {
                        // Crypto logic: Pull the broad category
                        return uriBuilder.path("/news")
                                .queryParam("category", "crypto")
                                .queryParam("token", apiKey)
                                .build();
                    } else {
                        // Stock logic: Pull symbol-specific news
                        return uriBuilder.path("/company-news")
                                .queryParam("symbol", symbol)
                                .queryParam("from", LocalDate.now().toString())
                                .queryParam("to", LocalDate.now().toString())
                                .queryParam("token", apiKey)
                                .build();
                    }
                })
                .retrieve()
                .bodyToFlux(NewsRecord.class)
                .filter(record -> isRelevant(record, symbol)) // filter noise
                .take(3) // top 3 per symbol to prevent flooding the NLP engine
                .map(record -> mapToEvent(record, symbol));
    }

    private boolean isRelevant(NewsRecord record, String symbol) {
        // If it's crypto, ensure the headline actually mentions our symbol
        // (e.g., don't process Dogecoin news if we only track BTC)
        return record.headline().toUpperCase().contains(symbol) ||
                record.related().equalsIgnoreCase(symbol);
    }

    private boolean isCrypto(String symbol) {
        return List.of("BTC", "ETH", "SOL").contains(symbol);
    }

    private MarketEvent mapToEvent(NewsRecord record, String symbol) {
        return new MarketEvent(symbol, 0.0, Instant.now(), 0.0, record.headline(), 0.0, 0.0, false);
    }
}




