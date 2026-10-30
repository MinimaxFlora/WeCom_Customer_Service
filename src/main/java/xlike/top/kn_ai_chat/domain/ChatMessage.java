package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @author xlike
 */
@Data
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum SenderType { ADMIN, USER }
    public enum MessageType { TEXT, IMAGE, FILE }

    // 始终是用户的ID
    @Column(nullable = false)
    private String externalUserId;

    @Column(length = 100)
    private String openKfid;

    @Column(length = 200)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // 标记是管理员还是用户发送
    private SenderType senderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // 消息类型
    private MessageType messageType;

    @Column(columnDefinition = "TEXT", nullable = false)
    // 对于文本是内容，对于文件/图片是URL或Base64
    private String content;

    @Column(columnDefinition = "TEXT")
    // 用于存储JSON格式的元数据 (如：文件名，文件大小)
    private String meta;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}