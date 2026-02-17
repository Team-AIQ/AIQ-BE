package cmc.aiq.aiq.dto.History;

import java.time.LocalDateTime;

public record HistoryResponseDTO(
        Long queryId,
        String question,
        LocalDateTime createdAt
) {
}
