package xlike.top.kn_ai_chat.handler;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.AiConfig;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.enums.MediaType;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.reply.VoiceReply;
import xlike.top.kn_ai_chat.service.AiService;
import xlike.top.kn_ai_chat.service.EnterpriseKnowledgeService;
import xlike.top.kn_ai_chat.service.EnterpriseProfileService;
import xlike.top.kn_ai_chat.service.FormatFileService;
import xlike.top.kn_ai_chat.service.ManualTransferService;
import xlike.top.kn_ai_chat.service.MediaService;
import xlike.top.kn_ai_chat.service.SemanticService;
import xlike.top.kn_ai_chat.service.SiliconFlowService;
import xlike.top.kn_ai_chat.service.UserConfigService;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 普通消息兜底：先检索企业知识，再结合历史调用模型。
 */
@Component
public class AiMessageHandler implements MessageHandler {

    private static final Set<String> BUSINESS_HINTS = Set.of(
            "价格", "多少钱", "费用", "报价", "售后", "保修", "维修", "退换", "退货", "退款",
            "发货", "物流", "快递", "几天", "产品", "商品", "型号", "规格", "公司", "企业",
            "地址", "电话", "营业", "发票", "合同", "定制", "服务", "保修期", "质保"
    );

    private final AiService aiService;
    private final SemanticService semanticService;
    private final SiliconFlowService siliconFlowService;
    private final MediaService mediaService;
    private final FormatFileService formatFileService;
    private final EnterpriseKnowledgeService knowledgeService;
    private final EnterpriseProfileService profileService;
    private final ManualTransferService manualTransferService;
    private final UserConfigService userConfigService;

    public AiMessageHandler(AiService aiService, SemanticService semanticService,
                            SiliconFlowService siliconFlowService, MediaService mediaService,
                            FormatFileService formatFileService,
                            EnterpriseKnowledgeService knowledgeService,
                            EnterpriseProfileService profileService,
                            ManualTransferService manualTransferService,
                            UserConfigService userConfigService) {
        this.aiService = aiService;
        this.semanticService = semanticService;
        this.siliconFlowService = siliconFlowService;
        this.mediaService = mediaService;
        this.formatFileService = formatFileService;
        this.knowledgeService = knowledgeService;
        this.profileService = profileService;
        this.manualTransferService = manualTransferService;
        this.userConfigService = userConfigService;
    }

    @Override
    public boolean canHandle(String content, String externalUserId) {
        return true;
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String knowledgeContext = knowledgeService.buildContext(content, 5);
        if ((knowledgeContext == null || knowledgeContext.isBlank()) && isBusinessQuestion(content)) {
            AiConfig config = userConfigService.getAiConfig(externalUserId);
            if ("AUTO".equalsIgnoreCase(config.getHandoffPolicy())) {
                manualTransferService.request(externalUserId, openKfid, content);
                return Optional.of(new TextReply(
                        "这个问题我暂时无法从企业资料中确认。为避免给您错误信息，已为您转接人工客服，请稍候。"));
            }
            return Optional.of(new TextReply(
                    "这个问题我暂时无法从企业资料中确认。为避免给您错误信息，您可以回复“转人工”，我帮您转接人工客服。"));
        }

        String textReply = aiService.getChatCompletionWithEnterpriseKnowledge(
                history, openKfid, knowledgeContext, profileService.get());

        if (textReply == null || textReply.isEmpty()) {
            return Optional.of(new TextReply("抱歉，我暂时无法回答这个问题，请稍后再试。"));
        }

        if (semanticService.getBooleanJudgement(content, externalUserId)) {
            Optional<File> mp3FileOpt = siliconFlowService.generateSpeech(textReply, externalUserId);

            if (mp3FileOpt.isPresent()) {
                Optional<File> amrFileOpt = formatFileService.convertToAmr(mp3FileOpt.get());
                mp3FileOpt.get().delete();

                if (amrFileOpt.isPresent()) {
                    Optional<String> mediaIdOpt = mediaService.uploadTemporaryMedia(amrFileOpt.get(), MediaType.VOICE);
                    amrFileOpt.get().delete();
                    if (mediaIdOpt.isPresent()) {
                        return Optional.of(new VoiceReply(mediaIdOpt.get()));
                    }
                }
            }
        }

        return Optional.of(new TextReply(textReply));
    }

    private boolean isBusinessQuestion(String content) {
        if (content == null) {
            return false;
        }
        return BUSINESS_HINTS.stream().anyMatch(content::contains);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
