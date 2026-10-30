package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "manual_transfer_request")
public class ManualTransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String externalUserId;

    @Column(length = 100)
    private String openKfid;

    @Column(length = 200)
    private String conversationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "externalUserId", referencedColumnName = "externalUserId", insertable = false, updatable = false)
    private WeChatUser user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String lastMessage;

    @Column(nullable = false)
    private LocalDateTime requestTime;

    @Column(nullable = false)
    private boolean resolved = false;
}