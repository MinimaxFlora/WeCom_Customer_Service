package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xlike.top.kn_ai_chat.domain.EnterpriseProfile;
import xlike.top.kn_ai_chat.repository.EnterpriseProfileRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

/**
 * 单企业部署下的企业资料服务。固定只有 id=default 一行。
 */
@Service
public class EnterpriseProfileService {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseProfileService.class);
    public static final String DEFAULT_ID = "default";

    private final EnterpriseProfileRepository repository;

    @Value("${wechat.corp-id:}")
    private String wechatCorpId;

    public EnterpriseProfileService(EnterpriseProfileRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void initDefaultProfile() {
        if (repository.existsById(DEFAULT_ID)) {
            return;
        }
        EnterpriseProfile profile = new EnterpriseProfile();
        profile.setId(DEFAULT_ID);
        profile.setName("我的企业");
        profile.setWechatCorpId(wechatCorpId);
        profile.setUpdatedAt(LocalDateTime.now());
        repository.save(profile);
        logger.info("已初始化默认企业资料");
    }

    public EnterpriseProfile get() {
        return repository.findById(DEFAULT_ID).orElseGet(() -> {
            initDefaultProfile();
            return repository.findById(DEFAULT_ID)
                    .orElseThrow(() -> new IllegalStateException("企业资料初始化失败"));
        });
    }

    @Transactional
    public EnterpriseProfile save(EnterpriseProfile input) {
        EnterpriseProfile profile = get();
        profile.setName(input.getName());
        profile.setIntro(input.getIntro());
        profile.setIndustry(input.getIndustry());
        profile.setAddress(input.getAddress());
        profile.setPhone(input.getPhone());
        profile.setWebsite(input.getWebsite());
        profile.setBusinessHours(input.getBusinessHours());
        profile.setServiceScope(input.getServiceScope());
        if (input.getWechatCorpId() != null && !input.getWechatCorpId().isBlank()) {
            profile.setWechatCorpId(input.getWechatCorpId());
        }
        if (input.getDefaultOpenKfid() != null && !input.getDefaultOpenKfid().isBlank()) {
            profile.setDefaultOpenKfid(input.getDefaultOpenKfid());
        }
        profile.setUpdatedAt(LocalDateTime.now());
        return repository.save(profile);
    }
}
