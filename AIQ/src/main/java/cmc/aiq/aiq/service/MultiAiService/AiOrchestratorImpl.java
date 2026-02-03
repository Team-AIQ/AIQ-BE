package cmc.aiq.aiq.service.MultiAiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Log4j2
public class AiOrchestratorImpl implements AiOrchestrator{
    @Override
    public void executeParallelAi(String userQuestion, String context, SseEmitter emitter) {

    }
}
