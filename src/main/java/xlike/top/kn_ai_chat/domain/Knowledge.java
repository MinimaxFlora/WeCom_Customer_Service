package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库条目实体类
 * <p>
 * 用于存储用户上传的文件内容，实现用户隔离的知识库。
 * @author xlike
 */
@Data
@Entity
@Table(name = "knowledge_base")
public class Knowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 个人知识库所属的 external_userid；企业公共知识为 null。
     */
    @Column(nullable = true, updatable = false)
    private String externalUserId;

    /**
     * 单企业部署固定为 default，后续多企业时按此字段过滤。
     */
    @Column(nullable = false, length = 64)
    private String enterpriseId = "default";

    /**
     * PERSONAL：客户个人文件；ENTERPRISE：企业公共知识。
     */
    @Column(nullable = false, length = 20)
    private String scope = "PERSONAL";

    /**
     * COMPANY / PRODUCT / PRESALES / AFTERSALES / FAQ / CUSTOM。
     */
    @Column(length = 30)
    private String category;

    /**
     * 知识条目标题；文件类默认使用文件名。
     */
    @Column(length = 500)
    private String title;

    /**
     * WECHAT_FILE / ADMIN_UPLOAD / MANUAL。
     */
    @Column(length = 30)
    private String sourceType;

    /**
     * PENDING / PROCESSING / READY / FAILED / DELETING。
     */
    @Column(nullable = false, length = 20)
    private String status = "READY";

    /**
     * 上传时的原始文件名。
     */
    @Column(nullable = false)
    private String fileName;

    /**
     * 从文件中提取的纯文本内容。
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 1000)
    private String storagePath;

    @Column(length = 200)
    private String mimeType;

    private Long fileSize;

    @Column(length = 2000)
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}