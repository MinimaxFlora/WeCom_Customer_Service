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

    public ManualTransferService(ManualTransferRepository repository,
                                 ManualModeService manualModeService,
                                 EmailService emailService) {
        this.repository = repository;
        this.manualModeService = manualModeService;
        this.emailService = emailService;
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
    public void resolve(String externalUserId) {
        repository.findByExternalUserId(externalUserId).ifPresent(request -> {
            request.setResolved(true);
            repository.save(request);
        });
        manualModeService.exit(externalUserId);
    }
}
