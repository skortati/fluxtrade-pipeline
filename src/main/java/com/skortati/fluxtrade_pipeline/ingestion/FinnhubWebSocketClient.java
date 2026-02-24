package com.skortati.fluxtrade_pipeline.ingestion;

import com.skortati.fluxtrade_pipeline.core.PipelineEngine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinnhubWebSocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinnhubWebSocketClient.class);
    private final PipelineEngine pipelineEngine;
    private final MockNewsService mockNewsService;

    @Value("${finnhub.api.key}")
    private String apiKey;

    private final FinnhubDataService dataService;
    // Using this client means the application can handle thousands of concurrent messages without creating a new thread for every message -> "scale out" architecture
    private final ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

    @PostConstruct
    public void connect() {
        String url = "wss://ws.finnhub.io?token=" + apiKey;
        List<String> symbolsToTrack = List.of("NVDA", "BINANCE:BTCUSDT");

        client.execute(URI.create(url), session -> {
                    Flux<WebSocketMessage> subscriptionMessages = Flux.fromIterable(symbolsToTrack)
                            .map(symbol -> "{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}")
                            .map(session::textMessage);

                    return session.send(subscriptionMessages)
                            .thenMany(session.receive()
                                    .map(WebSocketMessage::getPayloadAsText)
                                    .flatMap(dataService::parse) // Send raw JSON to your parser

                                    // [TESTING BLOCK]
                                            .map(event -> {
                                                String headline = mockNewsService.getHeadlineFor(event.symbol());
                                                return event.withHeadline(headline);
                                            })

                                    // backpressure: drop trades if the pipeline is overwhelmed
                                    .onBackpressureDrop(dropped -> LOGGER.warn("Backpressure: Dropping trade for {}", dropped.symbol()))
                                    // concurrency control: process up to 10 events in parallel
                                    // this prevents the slow SentimentPlugin from blocking the whole stream
                                    .flatMap(pipelineEngine::runPipeline, 10)
                                    .doOnNext(event -> LOGGER.info("Received Trade: {} at ${}", event.symbol(), event.price()))
                            )
                            .then();
                })
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(2))) // Auto-reconnect logic (wait -> backoff -> try to reconnect)
                .subscribe();
    }
}
