package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单企业部署下的企业资料。固定只有一行，id = "default"。
 */
@Data
@Entity
@Table(name = "enterprise_profile")
public class EnterpriseProfile {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @Column(length = 100)
    private String industry;

    @Column(length = 500)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 200)
    private String website;

    @Column(length = 200)
    private String businessHours;

    @Column(columnDefinition = "TEXT")
    private String serviceScope;

    @Column(length = 100)
    private String wechatCorpId;

    @Column(length = 100)
    private String defaultOpenKfid;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
