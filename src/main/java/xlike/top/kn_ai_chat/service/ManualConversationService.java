package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import xlike.top.kn_ai_chat.domain.ChatMessage;
import xlike.top.kn_ai_chat.handler.websocket.AdminWebSocketHandler;
import xlike.top.kn_ai_chat.repository.ChatMessageRepository;

import java.time.LocalDateTime;

@Service
public class ManualConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ManualConversationService.class);
    private final ChatMessageRepository chatMessageRepository;
    private final AdminWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public ManualConversationService(ChatMessageRepository chatMessageRepository,
                                     @Lazy AdminWebSocketHandler webSocketHandler) {
        this.chatMessageRepository = chatMessageRepository;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void saveAndPushUserMessage(String externalUserId, String openKfid, String content) {
        ChatMessage message = new ChatMessage();
        message.setExternalUserId(externalUserId);
        message.setOpenKfid(openKfid);
        message.setConversationId((openKfid == null ? "default" : openKfid) + ":" + externalUserId);
        message.setSenderType(ChatMessage.SenderType.USER);
        message.setMessageType(ChatMessage.MessageType.TEXT);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(message);
        try {
            webSocketHandler.sendMessageToAdmins(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            logger.warn("推送人工客服消息失败: {}", e.getMessage());
        }
    }
}
