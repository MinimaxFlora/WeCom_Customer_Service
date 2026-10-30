package xlike.top.kn_ai_chat.handler.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import xlike.top.kn_ai_chat.domain.ChatMessage;
import xlike.top.kn_ai_chat.repository.ChatMessageRepository;
import xlike.top.kn_ai_chat.service.WeChatKfAccountService;
import xlike.top.kn_ai_chat.service.WeChatService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author xlike
 */
@Component
public class AdminWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdminWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatMessageRepository chatMessageRepository;
    private final WeChatService weChatService;
    private final StringRedisTemplate redisTemplate;
    private final WeChatKfAccountService weChatKfAccountService;

    public AdminWebSocketHandler(ChatMessageRepository chatMessageRepository,
                                 WeChatService weChatService,
                                 StringRedisTemplate redisTemplate,
                                 WeChatKfAccountService weChatKfAccountService) {
        this.chatMessageRepository = chatMessageRepository;
        this.weChatService = weChatService;
        this.redisTemplate = redisTemplate;
        this.weChatKfAccountService = weChatKfAccountService;
    }


    /**
     * 向所有已连接的管理员客户端广播消息。
     *
     * @param messageJson 要发送的JSON格式的消息字符串。
     */
    public void sendMessageToAdmins(String messageJson) {
        TextMessage message = new TextMessage(messageJson);
        // 遍历所有会话并发送消息
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                logger.error("向管理员 session [{}] 发送消息失败", session.getId(), e);
            }
        });
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String adminId = "admin_" + session.getId();
        sessions.put(adminId, session);
        logger.info("管理员 WebSocket 连接建立: {}", adminId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        logger.debug("收到管理员消息，长度: {}", payload.length());

        try {
            Map<String, Object> msgData = objectMapper.readValue(payload, new TypeReference<>() {
            });
            String recipientId = (String) msgData.get("recipientId");
            String typeStr = (String) msgData.get("type");
            String content = (String) msgData.get("content");
            ChatMessage.MessageType messageType = ChatMessage.MessageType.valueOf(typeStr.toUpperCase());

            String userRedisKey = "manual_chat_mode:" + recipientId;
            redisTemplate.expire(userRedisKey, 30, TimeUnit.MINUTES);
            logger.info("管理员回复，已刷新用户 [{}] 的人工会话时长。", recipientId);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setExternalUserId(recipientId);
            chatMessage.setSenderType(ChatMessage.SenderType.ADMIN);
            chatMessage.setMessageType(messageType);
            chatMessage.setContent(content);
            chatMessage.setTimestamp(LocalDateTime.now());

            if (msgData.containsKey("meta")) {
                chatMessage.setMeta(objectMapper.writeValueAsString(msgData.get("meta")));
            }
            chatMessageRepository.save(chatMessage);
            String openKfid = weChatKfAccountService.getOpenKfid();
            switch (messageType) {
                case TEXT:
                    weChatService.sendTextMessage(recipientId, openKfid, content);
                    break;
                case IMAGE:
                    logger.info("模拟发送图片消息给 {},内容是：{}", recipientId, content);
                    break;
                case FILE:
                    logger.info("模拟发送文件消息给 {}", recipientId);
                    break;
            }

        } catch (Exception e) {
            logger.error("处理或发送管理员消息失败", e);
            session.sendMessage(new TextMessage("{\"error\":\"发送失败\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String adminId = "admin_" + session.getId();
        sessions.remove(adminId);
        logger.info("管理员 WebSocket 连接关闭: {}, 原因: {}", adminId, status);
    }
}