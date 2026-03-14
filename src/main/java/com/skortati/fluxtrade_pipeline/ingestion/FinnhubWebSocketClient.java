package com.skortati.fluxtrade_pipeline.ingestion;

import com.skortati.fluxtrade_pipeline.core.NewsCache;
import com.skortati.fluxtrade_pipeline.core.PipelineEngine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
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
    private final NewsCache newsCache;

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
                                    .map(message -> {
                                        DataBuffer payload = message.getPayload();
                                        // IMPORTANT: manually increment the reference count (refcnt)
                                        // -> tells Netty to not recycle this yet
                                        return DataBufferUtils.retain(payload);
                                    })
                                    // TRANSOFRMATION: parse bytes -> MarketEvent (Mono)
                                    .flatMap(buffer ->
                                            dataService.parseFromBuffer(buffer)
                                                    .doFinally(signal -> DataBufferUtils.release(buffer))
                                                    .doOnError(e -> LOGGER.error("Error parsing market event: {}", e.getMessage()))
                                                    .onErrorResume(e -> Mono.empty())) // ensure one bad packet doesnt kill the stream'
                                    // ENRICHMENT: add async new state
                                    .flatMap(event -> Mono.fromCallable(() -> {
                                        String headline = newsCache.getFor(event.symbol());
                                        return event.withHeadline(headline);
                                    }).subscribeOn(Schedulers.parallel()))
                                    // BACKPRESSURE: drop data if pipelin engine is bottlenecked
                                    .onBackpressureDrop(dropped -> LOGGER.warn("Backpressure: Dropping trade for {}", dropped.symbol()))
                                    // EXECUTION: Run the chain of responsibility (10 parallel rails)
                                    .flatMap(pipelineEngine::runPipeline, 10)
                                    .doOnNext(event -> LOGGER.info("Pipeline Complete: {} | Alert: {}",
                                            event.symbol(), event.isAlertTriggered())))
                            .then();
                })
                // RESILIENCE: if the socket drops, wait 2s, then 4s, then 8s... up to 10 times
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(2))
                        .doBeforeRetry(signal -> LOGGER.warn("WebSocket disconnected. Retrying...")))
                .subscribe();
    }
}
