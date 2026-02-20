package com.skortati.fluxtrade_pipeline.api;

import com.skortati.fluxtrade_pipeline.pipeline.SentimentPlugin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineController {
    private final SentimentPlugin sentimentPlugin;

    @PostMapping("/sentiment/toggle")
    public String toggleSentiment(@RequestParam boolean enabled) {
        sentimentPlugin.setEnabled(enabled);
        return "Sentiment Plugin is now " + (enabled ? "ENABLED" : "DISABLED");
    }

}
