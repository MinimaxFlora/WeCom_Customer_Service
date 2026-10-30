package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xlike.top.kn_ai_chat.domain.AiConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 可选的 Embedding 服务。没有配置 SiliconFlow/RAG Key 时返回 empty，
 * 检索层自动退化为关键词/字符匹配，保证功能可用。
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    private final RestTemplate restTemplate;
    private final UserConfigService userConfigService;

    public EmbeddingService(RestTemplate restTemplate, UserConfigService userConfigService) {
        this.restTemplate = restTemplate;
        this.userConfigService = userConfigService;
    }

    public boolean isAvailable() {
        AiConfig config = userConfigService.getAiConfig("default");
        return config.getRagApiKey() != null && !config.getRagApiKey().isBlank()
                && config.getRagBaseUrl() != null && !config.getRagBaseUrl().isBlank();
    }

    public Optional<float[]> embed(String text) {
        if (text == null || text.isBlank() || !isAvailable()) {
            return Optional.empty();
        }
        AiConfig config = userConfigService.getAiConfig("default");
        String baseUrl = config.getRagBaseUrl().replaceAll("/+$", "");
        String url = baseUrl.endsWith("/embeddings") ? baseUrl : baseUrl + "/embeddings";
        String model = config.getRagModel() == null || config.getRagModel().isBlank()
                ? "Qwen/Qwen3-Embedding-8B" : config.getRagModel();

        Map<String, Object> body = Map.of("model", model, "input", text);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);
            if (response == null) {
                return Optional.empty();
            }
            Object data = response.get("data");
            if (!(data instanceof List<?> list) || list.isEmpty()) {
                return Optional.empty();
            }
            Object first = list.get(0);
            if (!(first instanceof Map<?, ?> map)) {
                return Optional.empty();
            }
            Object embedding = map.get("embedding");
            if (!(embedding instanceof List<?> values) || values.isEmpty()) {
                return Optional.empty();
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = ((Number) values.get(i)).floatValue();
            }
            return Optional.of(vector);
        } catch (Exception e) {
            logger.warn("Embedding 调用失败，将使用关键词检索: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
