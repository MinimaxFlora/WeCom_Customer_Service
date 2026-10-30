package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xlike.top.kn_ai_chat.domain.ManualTransferRequest;
import xlike.top.kn_ai_chat.repository.ManualTransferRepository;

import java.util.List;

/**
 * 人工会话超时清理：Redis 人工状态已过期/应用重启丢失时，
 * 尝试把微信侧会话切回智能助手，避免一直卡在人工接待。
 */
@Service
public class ManualTimeoutService {

    private static final Logger logger = LoggerFactory.getLogger(ManualTimeoutService.class);
    private final ManualTransferRepository repository;
    private final ManualModeService manualModeService;
    private final ManualTransferService manualTransferService;

    public ManualTimeoutService(ManualTransferRepository repository,
                                ManualModeService manualModeService,
                                ManualTransferService manualTransferService) {
        this.repository = repository;
        this.manualModeService = manualModeService;
        this.manualTransferService = manualTransferService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void closeExpiredManualSessions() {
        List<ManualTransferRequest> pending = repository.findByResolvedFalseOrderByRequestTimeDesc();
        for (ManualTransferRequest request : pending) {
            String userId = request.getExternalUserId();
            if (!manualModeService.isActive(userId)) {
                logger.info("检测到人工状态已过期，尝试切回 AI: user={}", userId);
                manualTransferService.resolve(userId);
            }
        }
    }
}
