package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author xlike
 */
@Data
@Entity
@Table(name = "message_log")
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 微信消息的唯一ID，用于持久化幂等性判断
     */
    @Column(unique = true, nullable = true)
    private String msgId;

    private String fromUser;
    private String toUser;
    private String msgType;

    @Column(length = 100)
    private String openKfid;

    @Column(length = 200)
    private String conversationId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime timestamp;
}