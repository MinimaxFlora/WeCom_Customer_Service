package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import xlike.top.kn_ai_chat.config.SecretCryptoConverter;
import java.time.LocalDateTime;

/**
 * @author xlike
 */
@Data
@Entity
@Table(name = "ai_config")
public class AiConfig {

    @Id
    private String externalUserId;

    @Column(nullable = false)
    private String aiBaseUrl;

    @Convert(converter = SecretCryptoConverter.class)
    @Column(nullable = false)
    private String aiApiKey;

    @Column(nullable = false)
    private String aiModel;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String systemPrompt;

    @Column(nullable = false)
    private String sfBaseUrl;

    @Column(nullable = false)
    private String sfImageModel;

    @Column(nullable = false)
    private String sfTtsModel;

    @Column(nullable = false)
    private String sfSttModel;

    @Column(nullable = false)
    private String sfVoice;

    @Column(nullable = false)
    private String sfVlmModel;

    @Column(nullable = false)
    private boolean ragEnabled = false;

    // 新增Rag字段
    @Column
    private String ragModel;

    @Column
    private String ragBaseUrl;

    @Convert(converter = SecretCryptoConverter.class)
    @Column
    private String ragApiKey;


    @Column(nullable = false)
    private double temperature = 0.3;

    @Column(nullable = false)
    private int maxTokens = 2000;

    /**
     * STRICT：严格依据企业资料；BALANCED：资料不足时允许通用回答。
     */
    @Column(nullable = false, length = 20)
    private String answerPolicy = "STRICT";

    /**
     * SUGGEST：建议转人工；AUTO：自动创建人工请求。
     */
    @Column(nullable = false, length = 20)
    private String handoffPolicy = "SUGGEST";

    @Column(nullable = false)
    private LocalDateTime lastModified;
}