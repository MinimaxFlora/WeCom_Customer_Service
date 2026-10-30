package xlike.top.kn_ai_chat.service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import xlike.top.kn_ai_chat.domain.AiConfig;
import xlike.top.kn_ai_chat.mcp.Bot;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.tools.tool.BraveSearchTool;
import xlike.top.kn_ai_chat.tools.tool.EmailTool;
import xlike.top.kn_ai_chat.tools.tool.GoogleSearchTool;
import xlike.top.kn_ai_chat.tools.tool.NotionTool;
import xlike.top.kn_ai_chat.utils.MarkdownCleanerUtil;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 */
@Service
public class AdminService {

    private static final String ADMIN_SESSION_KEY_PREFIX = "admin_session:";
    private static final long ADMIN_SESSION_TIMEOUT_DAYS = 7; 
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final Map<String, ChatMemory> userChatMemories = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;
    private final UserConfigService userConfigService;
    private final BraveSearchTool braveSearchTool;
    private final EmailTool emailTool;
    private final String adminPassword;
    private final GoogleSearchTool googleSearchTool;
    private final NotionTool notionTool;

    public AdminService(StringRedisTemplate redisTemplate,
                        UserConfigService userConfigService,
                        BraveSearchTool braveSearchTool,
                        EmailTool emailTool,
                        GoogleSearchTool googleSearchTool,
                        NotionTool notionTool,
                        @Value("${admin.password}") String adminPassword) {
        this.redisTemplate = redisTemplate;
        this.userConfigService = userConfigService;
        this.braveSearchTool = braveSearchTool;
        this.emailTool = emailTool;
        this.googleSearchTool = googleSearchTool;
        this.notionTool = notionTool;
        this.adminPassword = adminPassword;
    }

    public Reply authenticate(String externalUserId, String password) {
        if (adminPassword.equals(password)) {
            String redisKey = ADMIN_SESSION_KEY_PREFIX + externalUserId;
            redisTemplate.opsForValue().set(redisKey, "true", ADMIN_SESSION_TIMEOUT_DAYS, TimeUnit.DAYS);
            userChatMemories.remove(externalUserId);
            return new TextReply("✅ 认证成功！获得管理员权限" + ADMIN_SESSION_TIMEOUT_DAYS + "天。");
        } else {
            return new TextReply("❌ 认证失败：密码错误。");
        }
    }

    public boolean isAdmin(String externalUserId) {
        String redisKey = ADMIN_SESSION_KEY_PREFIX + externalUserId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }
    
    public Reply logout(String externalUserId) {
        String redisKey = ADMIN_SESSION_KEY_PREFIX + externalUserId;
        Boolean deleted = redisTemplate.delete(redisKey);
        if (Boolean.TRUE.equals(deleted)) {
            userChatMemories.remove(externalUserId);
            log.info("管理员 [{}] 已退出登录。", externalUserId);
            return new TextReply("✅ 您已成功退出管理员模式。");
        } else {
            return new TextReply("ℹ️ 您当前未处于管理员模式。");
        }
    }
        
    public Reply updateConfig(String key, String value) {
        AiConfig defaultConfig = userConfigService.getAiConfig("default");
        boolean updated = true;
        switch (key.toLowerCase()) {
            case "base_url":
                defaultConfig.setAiBaseUrl(value);
                break;
            case "model":
                defaultConfig.setAiModel(value);
                break;
            default:
                updated = false;
        }

        if (updated) {
            userConfigService.saveOrUpdateAiConfig(defaultConfig);
            return new TextReply("✅ 全局配置 '" + key + "' 已更新为: " + value);
        } else {
            return new TextReply("❌ 未知的配置项: " + key + "。支持的配置项：base_url, model。");
        }
    }


    /**
     * 为管理员执行工具调用。
     * AI Agent会根据用户问题智能选择合适的工具。
     * @param externalUserId 用户的外部ID
     * @param content 用户输入的指令
     * @return 包含AI工具调用结果的 Reply 对象
     */
    public Reply executeTool(String externalUserId, String content) {
        AiConfig aiConfig = userConfigService.getAiConfig(externalUserId);
        String aiBaseUrl = aiConfig.getAiBaseUrl();
        if (aiBaseUrl.endsWith("/chat/completions")) {
            aiBaseUrl = aiBaseUrl.substring(0, aiBaseUrl.lastIndexOf("/chat/completions"));
        }

        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .baseUrl(aiBaseUrl)
                .apiKey(aiConfig.getAiApiKey())
                .modelName(aiConfig.getAiModel())
                .maxTokens(1500)
                .logRequests(true)
                .logResponses(true)
                .build();
                
        ChatMemory chatMemory = userChatMemories.computeIfAbsent(
                externalUserId,
                id -> MessageWindowChatMemory.withMaxMessages(10)
        );
        Bot bot = AiServices.builder(Bot.class)
                .chatModel(openAiChatModel)
                .tools(googleSearchTool, braveSearchTool, emailTool, notionTool)
                .chatMemory(chatMemory)
                .build();
                
        String response = bot.chat(content);
        return new TextReply(MarkdownCleanerUtil.cleanMarkdown(response));
    }
}