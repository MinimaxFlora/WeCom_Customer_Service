package xlike.top.kn_ai_chat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.ManualConversationService;
import xlike.top.kn_ai_chat.service.ManualModeService;
import xlike.top.kn_ai_chat.service.ManualTransferService;
import xlike.top.kn_ai_chat.service.UserConfigService;

import java.util.List;
import java.util.Optional;

/**
 * 人工接管处理器。优先级最高，确保“转人工/结束人工”不会被自定义回复抢先。
 */
@Component
public class ManualChatHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ManualChatHandler.class);
    private final ManualModeService manualModeService;
    private final ManualTransferService manualTransferService;
    private final ManualConversationService conversationService;
    private final UserConfigService userConfigService;

    public ManualChatHandler(ManualModeService manualModeService,
                             ManualTransferService manualTransferService,
                             ManualConversationService conversationService,
                             UserConfigService userConfigService) {
        this.manualModeService = manualModeService;
        this.manualTransferService = manualTransferService;
        this.conversationService = conversationService;
        this.userConfigService = userConfigService;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean canHandle(String content, String externalUserId) {
        String trimmed = content == null ? "" : content.trim();
        boolean enter = userConfigService.getKeywordsForHandler(externalUserId, "ManualChatHandler_Enter")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(trimmed));
        boolean exit = userConfigService.getKeywordsForHandler(externalUserId, "ManualChatHandler_Exit")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(trimmed));
        return enter || exit || manualModeService.isActive(externalUserId);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String trimmed = content == null ? "" : content.trim();
        boolean exit = userConfigService.getKeywordsForHandler(externalUserId, "ManualChatHandler_Exit")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(trimmed));
        if (exit) {
            manualTransferService.resolve(externalUserId);
            logger.info("用户 [{}] 结束人工服务", externalUserId);
            return Optional.of(new TextReply("您已结束人工服务，现在将由智能小助手继续为您服务。"));
        }

        boolean enter = userConfigService.getKeywordsForHandler(externalUserId, "ManualChatHandler_Enter")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(trimmed));
        if (enter) {
            manualTransferService.request(externalUserId, openKfid, content);
            logger.info("用户 [{}] 进入人工服务模式", externalUserId);
            return Optional.of(new TextReply(
                    "您好，已为您转接人工客服，目前人工客服暂时只能处理文字形式的问题，请直接发送您的问题。如需结束，请发送“结束人工服务”。"));
        }

        if (manualModeService.isActive(externalUserId)) {
            conversationService.saveAndPushUserMessage(externalUserId, openKfid, content);
            manualModeService.refresh(externalUserId);
            return Optional.empty();
        }
        return Optional.empty();
    }
}
