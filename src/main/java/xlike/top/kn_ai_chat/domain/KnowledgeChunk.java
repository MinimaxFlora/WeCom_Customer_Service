package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库切片及其向量。单企业场景下 enterpriseId 固定为 "default"，
 * 后续多企业时可直接按该字段过滤。
 */
@Data
@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long knowledgeId;

    @Column(nullable = false, length = 64)
    private String enterpriseId;

    @Column(nullable = false, length = 20)
    private String scope;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * float[] 序列化后的向量。单企业先用 BLOB + Java 余弦检索。
     */
    @Lob
    @Column
    private byte[] embedding;

    @Column
    private Integer tokenCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
