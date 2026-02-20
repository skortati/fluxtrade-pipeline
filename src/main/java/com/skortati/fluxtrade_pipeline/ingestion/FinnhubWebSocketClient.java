package com.skortati.fluxtrade_pipeline.ingestion;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FinnhubWebSocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinnhubWebSocketClient.class);

    @Value("${finnhub.api.key}")
    private String apiKey;

    private final FinnhubDataService dataService;
    // Using this client means the application can handle thousands of concurrent messages without creating a new thread for every message -> "scale out" architecture
    private final ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

    @PostConstruct
    public void connect() {
        String url = "wss://ws.finnhub.io?token=" + apiKey;

        client.execute(URI.create(url), session ->
                        session.send(Mono.just(session.textMessage("{\"type\":\"subscribe\",\"symbol\":\"BINANCE:BTCUSDT\"}")))
                                .thenMany(session.receive()
                                        .map(WebSocketMessage::getPayloadAsText)
                                        .flatMap(dataService::parse) // Send raw JSON to your parser
                                        .doOnNext(event -> LOGGER.info("Received Trade: {} at ${}", event.symbol(), event.price()))
                                )
                                .then()
                )
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(2))) // Auto-reconnect logic (wait -> backoff -> try to reconnect)
                .subscribe();
    }
}
