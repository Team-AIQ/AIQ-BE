package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.service.MultiAiService.AiOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aiq")
@RequiredArgsConstructor
public class AiOrchestratorController {
    private final AiOrchestratorService aiOrchestratorService;

    @GetMapping(value = "/stream/{queryId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAiRecommendations(
            @PathVariable Long queryId,
            @RequestParam(required = false, defaultValue = "GPT,Gemini,Perplexity") List<String> models) {

        SseEmitter emitter = new SseEmitter(300_000L);

        aiOrchestratorService.executeParallelAi(queryId, models, emitter);

        return emitter;
    }
}
