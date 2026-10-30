package xlike.top.kn_ai_chat.dto;

import lombok.Data;
import xlike.top.kn_ai_chat.domain.AiConfig;

import java.time.LocalDateTime;

/**
 * 管理端返回的 AI 配置视图：密钥只返回掩码，避免明文回显。
 */
@Data
public class AiConfigView {
    private String externalUserId;
    private String aiBaseUrl;
    private String aiApiKey;
    private String aiModel;
    private String systemPrompt;
    private String sfBaseUrl;
    private String sfImageModel;
    private String sfTtsModel;
    private String sfSttModel;
    private String sfVoice;
    private String sfVlmModel;
    private boolean ragEnabled;
    private String ragModel;
    private String ragBaseUrl;
    private String ragApiKey;
    private double temperature;
    private int maxTokens;
    private String answerPolicy;
    private String handoffPolicy;
    private LocalDateTime lastModified;

    public static AiConfigView from(AiConfig config) {
        AiConfigView view = new AiConfigView();
        view.setExternalUserId(config.getExternalUserId());
        view.setAiBaseUrl(config.getAiBaseUrl());
        view.setAiApiKey(mask(config.getAiApiKey()));
        view.setAiModel(config.getAiModel());
        view.setSystemPrompt(config.getSystemPrompt());
        view.setSfBaseUrl(config.getSfBaseUrl());
        view.setSfImageModel(config.getSfImageModel());
        view.setSfTtsModel(config.getSfTtsModel());
        view.setSfSttModel(config.getSfSttModel());
        view.setSfVoice(config.getSfVoice());
        view.setSfVlmModel(config.getSfVlmModel());
        view.setRagEnabled(config.isRagEnabled());
        view.setRagModel(config.getRagModel());
        view.setRagBaseUrl(config.getRagBaseUrl());
        view.setRagApiKey(mask(config.getRagApiKey()));
        view.setTemperature(config.getTemperature());
        view.setMaxTokens(config.getMaxTokens());
        view.setAnswerPolicy(config.getAnswerPolicy());
        view.setHandoffPolicy(config.getHandoffPolicy());
        view.setLastModified(config.getLastModified());
        return view;
    }

    private static String mask(String secret) {
        if (secret == null || secret.isBlank()) {
            return "";
        }
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 3) + "****" + secret.substring(secret.length() - 4);
    }
}
