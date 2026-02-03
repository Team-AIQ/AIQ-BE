package cmc.aiq.aiq.service.MultiAiService;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiOrchestrator {
    void executeParallelAi(String userQuestion, String context, SseEmitter emitter);
}
