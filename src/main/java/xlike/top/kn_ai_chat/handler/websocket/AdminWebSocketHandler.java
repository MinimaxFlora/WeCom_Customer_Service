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
import xlike.top.kn_ai_chat.enums.MediaType;
import xlike.top.kn_ai_chat.repository.ChatMessageRepository;
import xlike.top.kn_ai_chat.service.MediaService;
import xlike.top.kn_ai_chat.service.WeChatKfAccountService;
import xlike.top.kn_ai_chat.service.WeChatService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Base64;
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
    private final MediaService mediaService;

    public AdminWebSocketHandler(ChatMessageRepository chatMessageRepository,
                                 WeChatService weChatService,
                                 StringRedisTemplate redisTemplate,
                                 WeChatKfAccountService weChatKfAccountService,
                                 MediaService mediaService) {
        this.chatMessageRepository = chatMessageRepository;
        this.weChatService = weChatService;
        this.redisTemplate = redisTemplate;
        this.weChatKfAccountService = weChatKfAccountService;
        this.mediaService = mediaService;
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
                    sendMedia(recipientId, openKfid, content, msgData.get("meta"), MediaType.IMAGE);
                    break;
                case FILE:
                    sendMedia(recipientId, openKfid, content, msgData.get("meta"), MediaType.FILE);
                    break;
            }

        } catch (Exception e) {
            logger.error("处理或发送管理员消息失败", e);
            String error = e.getMessage() == null ? "发送失败" : e.getMessage();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("error", error))));
        }
    }

    private void sendMedia(String recipientId, String openKfid, String dataUrl, Object metaObj,
                           MediaType mediaType) throws IOException {
        String mediaId = uploadDataUrl(dataUrl, metaObj, mediaType);
        boolean sent = mediaType == MediaType.IMAGE
                ? weChatService.sendImageMessage(recipientId, openKfid, mediaId)
                : weChatService.sendFileMessage(recipientId, openKfid, mediaId);
        if (!sent) {
            throw new IOException("微信客服拒绝发送 " + mediaType.getTypeName() + " 消息");
        }
        logger.info("已通过微信客服发送 {} 消息给 {}", mediaType.getTypeName(), recipientId);
    }

    private String uploadDataUrl(String dataUrl, Object metaObj, MediaType mediaType) throws IOException {
        if (dataUrl == null || !dataUrl.startsWith("data:")) {
            throw new IOException("媒体内容不是 data URL");
        }
        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            throw new IOException("媒体 data URL 格式错误");
        }
        String header = dataUrl.substring(0, comma).toLowerCase();
        byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
        long maxBytes = mediaType == MediaType.IMAGE ? 2L * 1024 * 1024 : 20L * 1024 * 1024;
        if (bytes.length > maxBytes) {
            throw new IOException("媒体文件超过微信客服限制: " + bytes.length + " bytes");
        }

        String fileName = "";
        if (metaObj instanceof Map<?, ?> meta) {
            Object value = meta.get("fileName");
            if (value != null) {
                fileName = value.toString();
            }
        }
        String extension = extensionOf(fileName, header);
        File temp = File.createTempFile("admin-media-", extension);
        try {
            Files.write(temp.toPath(), bytes);
            return mediaService.uploadTemporaryMedia(temp, mediaType)
                    .orElseThrow(() -> new IOException("微信客服临时素材上传失败"));
        } finally {
            Files.deleteIfExists(temp.toPath());
        }
    }

    private String extensionOf(String fileName, String dataHeader) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            return fileName.substring(dot);
        }
        if (dataHeader.contains("png")) return ".png";
        if (dataHeader.contains("jpeg") || dataHeader.contains("jpg")) return ".jpg";
        if (dataHeader.contains("gif")) return ".gif";
        if (dataHeader.contains("pdf")) return ".pdf";
        if (dataHeader.contains("zip")) return ".zip";
        return ".bin";
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String adminId = "admin_" + session.getId();
        sessions.remove(adminId);
        logger.info("管理员 WebSocket 连接关闭: {}, 原因: {}", adminId, status);
    }
}