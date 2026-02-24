package com.skortati.fluxtrade_pipeline.plugins;

import com.skortati.fluxtrade_pipeline.core.TradePlugin;
import com.skortati.fluxtrade_pipeline.model.MarketEvent;
import edu.stanford.nlp.pipeline.CoreDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import reactor.core.scheduler.Schedulers;

import java.util.Properties;

@Component
@Order(1)
public class SentimentPlugin implements TradePlugin {
    private final StanfordCoreNLP pipeline;

    public SentimentPlugin() {
        Properties props = new Properties();
        // We only need the bare minimum for sentiment to keep it "fast"
        props.setProperty("annotators", "tokenize, ssplit, pos, parse, sentiment");
        this.pipeline = new StanfordCoreNLP(props);
    }

    @Override
    public Mono<MarketEvent> process(MarketEvent event) {
        if (event.headline() == null || event.headline().isBlank()) {
            return Mono.just(event);
        }

        return Mono.fromCallable(() -> {
            CoreDocument doc = new CoreDocument(event.headline());
            pipeline.annotate(doc);

            double avgScore = doc.sentences().stream()
                    .mapToDouble(s -> switch (s.sentiment()) {
                        case "Very Positive" -> 1.0;
                        case "Positive" -> 0.5;
                        case "Negative" -> -0.5;
                        case "Very Negative" -> -1.0;
                        default -> 0.0;
                    })
                    .average()
                    .orElse(0.0);
            return event.withSentiment(avgScore);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
