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

        // 타임아웃 설정 (3개 모델 + 리포트 생성까지 시간이 걸리므로 5분 정도로 넉넉하게 설정)
        SseEmitter emitter = new SseEmitter(300_000L);

        // 비동기 서비스 호출
        aiOrchestratorService.executeParallelAi(queryId, models, emitter);

        return emitter;
    }
}
