package xlike.top.kn_ai_chat.dto;

import java.time.LocalDateTime;

public record KnowledgeSummary(
        Long id,
        String title,
        String category,
        String sourceType,
        String status,
        String fileName,
        Long fileSize,
        String errorMessage,
        LocalDateTime updatedAt
) {
}
