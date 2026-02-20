package com.skortati.fluxtrade_pipeline.ingestion;

import com.skortati.fluxtrade_pipeline.model.FinnhubResponse;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class FinnhubDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinnhubDataService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Transforms a raw JSON string from the WebSocket into a Flux of MarketEvents.
     * 1. Parses the JSON into the FinnhubResponse "envelope"
     * 2. Filters out non-trade messages (like pings).
     * 3. Flattens the list of TradeTicks into a stream.
     * 4. Maps each Tick to our internal MarketEvent.
     */
    public Flux<MarketEvent> parse(String json){
        return Mono.fromCallable(() -> objectMapper.readValue(json, FinnhubResponse.class)) // ensures non-block parsing -> readValue inside Mono -> moved to a different thread pool (scheduler) without blocking the network thread
                .filter(response -> "trade".equals(response.type()) && response.data() != null)
                .flatMapMany(response -> Flux.fromIterable(response.data()))
                .map(MarketEvent::fromTick)
                .doOnError(e -> LOGGER.error("Error parsing market event: {}", e.getMessage()))
                .onErrorResume(e -> Flux.empty()); // ensure one bad packet doesn't kill the stream
    }

}
