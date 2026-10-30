package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xlike.top.kn_ai_chat.domain.AiConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xlike
 */
@Service
public class SemanticService {

    private static final Logger logger = LoggerFactory.getLogger(SemanticService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserConfigService userConfigService;

    private static final String JUDGMENT_PROMPT = "请根据以下要求生成响应：1.深入分析用户输入的上下文、语气、语义及潜在意图，综合判断是否满足肯定条件，例如上下文是否需要使用语音回答，如果需要语音回答是true,否则是false。2.返回一个布尔值（true或false），true表示肯定判断，false表示否定或无法明确肯定。3.响应必须严格为以下JSON格式：{'result':true}或{'result':false}，无换行、注释或其他内容。4.若输入模糊、语义不清或无法判断，优先返回{'result':false}。示例：你觉得我今天心情很好吗？（语气积极），输出：{'result':true}-输入：明天会下雨吗？（无明确依据），输出：{'result':false}，对于一般的提问，都是false，当如果感觉到情绪悲伤,有波动的话，是true,请处理以下输入并返回结果：";


    public SemanticService(RestTemplate restTemplate, UserConfigService userConfigService) {
        this.restTemplate = restTemplate;
        this.userConfigService = userConfigService;
    }

    public boolean getBooleanJudgement(String userInput, String externalUserId) {
        AiConfig aiConfig = userConfigService.getAiConfig(externalUserId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getAiApiKey());

        Map<String, String> assistantMessage = Map.of("role", "system", "content", JUDGMENT_PROMPT + "{" + userInput + "}");
        Map<String, String> userMessage = Map.of("role", "user", "content", userInput);
        List<Map<String, String>> messages = Arrays.asList(assistantMessage, userMessage);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiConfig.getAiModel());
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            String responseStr = restTemplate.postForObject(aiConfig.getAiBaseUrl(), requestEntity, String.class);
            logger.debug("语义服务API响应长度: {}", responseStr == null ? 0 : responseStr.length());

            JsonNode root = objectMapper.readTree(responseStr);
            if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                String contentStr = root.get("choices").get(0).get("message").get("content").asText();

                try {
                    // 使用JSON解析
                    JsonNode contentJson = objectMapper.readTree(contentStr);
                    if (contentJson.isObject() && contentJson.has("result")) {
                        logger.info("成功通过标准JSON解析获取 'result'");
                        return contentJson.get("result").asBoolean(false);
                    }
                } catch (JsonProcessingException e) {
                    // JSON解析失败，启用正则解析
                    logger.warn("标准JSON解析失败: '{}'。将尝试使用正则表达式进行提取。", e.getMessage());
                    Pattern pattern = Pattern.compile("['\"]result['\"]\\s*:\\s*(true|false)");
                    Matcher matcher = pattern.matcher(contentStr);
                    if (matcher.find()) {
                        // group(1) 会捕获 (true|false)
                        String result = matcher.group(1);
                        logger.info("成功通过正则表达式后备方案获取 'result' 为 ： {}", result);
                        return Boolean.parseBoolean(result);
                    } else {
                        logger.error("正则表达式也无法从 content 中解析出 'result' : {}", contentStr);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("调用或解析语义服务API时发生顶层异常", e);
        }

        return false;
    }
}