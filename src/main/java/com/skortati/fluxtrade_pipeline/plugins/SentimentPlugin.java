package com.skortati.fluxtrade_pipeline.plugins;

import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import com.skortati.fluxtrade_pipeline.core.TradePlugin;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Order(1)
public class SentimentPlugin implements TradePlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SentimentPlugin.class);
    private ZooModel<String, Classifications> model;

    // runs after Spring finished DI but before the bean is put into service
    // it ensures the model is fully loaded into memory so that the first trade tick
    // doesn't experience a delay
    @PostConstruct
    public void init() throws Exception {
        // Load DistilBERT from HuggingFace via DJL Zoo
        Criteria<String, Classifications> criteria = Criteria.builder()
                .setTypes(String.class, Classifications.class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/distilbert-base-uncased-finetuned-sst-2-english")
                .optEngine("PyTorch")
                .optProgress(new ProgressBar())
                .build();

        // wait until the model is ready
        this.model = criteria.loadModel();

        LOGGER.info("[SentimentPlugin] Model loaded successfully.");
    }

    @Override
    public Mono<MarketEvent> process(MarketEvent event) {
        if (event.headline() == null || event.headline().isBlank()) return Mono.just(event);
        if (this.model == null) {
            LOGGER.warn("[SentimentPlugin] Model not loaded yet. Skipping sentiment for: {}", event.symbol());
            return Mono.just(event);
        }

        return Mono.fromCallable(() -> {
            // Predictors are not thread-safe; use try-with-resources to close them immediately
            try (Predictor<String, Classifications> predictor = model.newPredictor()) {
                Classifications result = predictor.predict(event.headline());

                // Get the top classification
                Classifications.Classification best = result.best();
                String label = best.getClassName().toUpperCase();
                double probability = best.getProbability();

                // Handle both "POSITIVE/NEGATIVE" and "LABEL_1/LABEL_0" formats
                double finalScore;
                if (label.contains("POSITIVE")) {
                    finalScore = probability; // 0.5 to 1.0
                } else {
                    finalScore = -probability; // -0.5 to -1.0
                }

                LOGGER.info("[SentimentPlugin] Sentiment for {}: {} ({}%)", event.symbol(), label, (int) (probability * 100));

                return event.withSentiment(finalScore);
            }
        }).subscribeOn(Schedulers.boundedElastic()); // DL inference is CPU-heavy -> boundedElastic -> create many threads
    }

    // Purpose: native memory cleanup - DJL uses C++ binaries via JNI, the model.close() call is mandatory
    // If we don't use this, the native pointers to RAM might lead to memory leaks
    @PreDestroy
    public void cleanup() {
        if (model != null) model.close();
    }

}
