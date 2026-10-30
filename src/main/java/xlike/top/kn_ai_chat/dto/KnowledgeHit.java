package xlike.top.kn_ai_chat.dto;

public record KnowledgeHit(
        Long knowledgeId,
        String title,
        String category,
        String content,
        double score
) {
}
