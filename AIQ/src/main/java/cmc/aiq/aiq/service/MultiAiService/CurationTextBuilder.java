package cmc.aiq.aiq.service.MultiAiService;

import cmc.aiq.aiq.domain.CurationSessions;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CurationTextBuilder {
    // 사용자 큐레이션 내용을 문자열로 치환하기 위한 클래스
    public String build(CurationSessions session) {
        if (session.getCurationResults() == null) {
            return "제공된 선호도 정보가 없습니다.";
        }

        // 답변이 있는 항목만 필터링해서 텍스트로 조립
        String context = session.getCurationResults().stream()
                .filter(res -> res.getUserAnswer() != null && !res.getUserAnswer().isBlank())
                .map(res -> String.format("- 질문: %s | 답변: %s",
                        res.getQuestionText(),
                        res.getUserAnswer()))
                .collect(Collectors.joining("\n"));

        return context.isEmpty() ? "제공된 상세 답변이 없습니다." : context;
    }
}
