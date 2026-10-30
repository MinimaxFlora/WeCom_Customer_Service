package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xlike.top.kn_ai_chat.domain.ManualTransferRequest;
import xlike.top.kn_ai_chat.repository.ManualTransferRepository;

import java.time.LocalDateTime;

@Service
public class ManualTransferService {

    private static final Logger logger = LoggerFactory.getLogger(ManualTransferService.class);
    private final ManualTransferRepository repository;
    private final ManualModeService manualModeService;
    private final EmailService emailService;
    private final WeChatServiceStateService serviceStateService;
    private final WeChatKfAccountService kfAccountService;

    public ManualTransferService(ManualTransferRepository repository,
                                 ManualModeService manualModeService,
                                 EmailService emailService,
                                 WeChatServiceStateService serviceStateService,
                                 WeChatKfAccountService kfAccountService) {
        this.repository = repository;
        this.manualModeService = manualModeService;
        this.emailService = emailService;
        this.serviceStateService = serviceStateService;
        this.kfAccountService = kfAccountService;
    }

    @Transactional
    public void request(String externalUserId, String openKfid, String content) {
        ManualTransferRequest request = repository.findByExternalUserId(externalUserId)
                .orElseGet(ManualTransferRequest::new);
        boolean isNew = request.getId() == null;
        request.setExternalUserId(externalUserId);
        request.setOpenKfid(openKfid);
        request.setConversationId(manualModeService.conversationId(openKfid, externalUserId));
        request.setLastMessage(content);
        request.setRequestTime(LocalDateTime.now());
        request.setResolved(false);
        repository.save(request);
        manualModeService.enter(externalUserId);

        if (isNew) {
            try {
                emailService.sendEmail("admin@example.com", "新的人工客服请求",
                        "用户 " + externalUserId + " 请求人工服务。");
            } catch (Exception e) {
                logger.warn("人工客服邮件通知失败: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public WeChatServiceStateService.StateResult resolve(String externalUserId) {
        ManualTransferRequest request = repository.findByExternalUserId(externalUserId).orElse(null);
        String openKfid = request != null && request.getOpenKfid() != null && !request.getOpenKfid().isBlank()
                ? request.getOpenKfid() : kfAccountService.getOpenKfid();
        WeChatServiceStateService.StateResult result = serviceStateService.backToAi(externalUserId, openKfid);
        if (!result.success()) {
            logger.warn("微信侧会话未能切回 AI，用户 [{}]，errcode={}。请在微信客服工作台手动结束会话。",
                    externalUserId, result.errcode());
        }
        if (request != null) {
            request.setResolved(true);
            repository.save(request);
        }
        manualModeService.exit(externalUserId);
        return result;
    }
}
