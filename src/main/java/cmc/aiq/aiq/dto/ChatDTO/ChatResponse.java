package cmc.aiq.aiq.dto.ChatDTO;

import java.util.List;

public record ChatResponse(
        String answer,
        List<String> nextSuggestedQuestions,
        int currentRound,
        boolean isLastRound
) {
}
