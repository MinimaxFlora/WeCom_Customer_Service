package xlike.top.kn_ai_chat.dto;

import lombok.Data;

@Data
public class KnowledgeRequest {
    private String title;
    private String category;
    private String content;
    private Boolean enabled;
}
