package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xlike.top.kn_ai_chat.domain.Knowledge;
import xlike.top.kn_ai_chat.repository.KnowledgeBaseRepository;
import xlike.top.kn_ai_chat.utils.FileContentReader;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 知识库服务
 * <p>
 * 封装对用户知识库的所有操作。
 * @author xlike
 */
@Service
public class KnowledgeBaseService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    private final KnowledgeBaseRepository repository;

    public KnowledgeBaseService(KnowledgeBaseRepository repository) {
        this.repository = repository;
    }

    /**
     * 将文件内容添加到指定用户的知识库中
     *
     * @param file           要处理的文件
     * @param fileName       原始文件名
     * @param externalUserId 用户ID
     * @return 操作结果的描述文本
     */
    public String addFileToKnowledgeBase(File file, String fileName, String externalUserId) {
        try {
            String content = FileContentReader.readFileContent(file);
            if (content.isBlank()) {
                return "文件 '" + fileName + "' 内容为空，已跳过。";
            }
            Knowledge entry = new Knowledge();
            entry.setExternalUserId(externalUserId);
            entry.setEnterpriseId("default");
            entry.setScope("PERSONAL");
            entry.setSourceType("WECHAT_FILE");
            entry.setStatus("READY");
            entry.setTitle(fileName);
            entry.setFileName(fileName);
            entry.setContent(content);
            entry.setCreatedAt(LocalDateTime.now());
            entry.setUpdatedAt(LocalDateTime.now());

            repository.save(entry);
            logger.info("用户 [{}] 的文件 '{}' 已成功存入知识库。", externalUserId, fileName);
            return "✅ 文件 '" + fileName + "' 已成功添加到您的知识库！";
        } catch (Exception e) {
            logger.error("为用户 [{}] 添加文件 '{}' 到知识库失败。", externalUserId, fileName, e);
            return "❌ 文件 '" + fileName + "' 添加失败：" + e.getMessage();
        }
    }

    /**
     * 直接返回知识库条目列表，供API使用
     *
     * @param externalUserId 用户ID
     * @return 知识库条目列表
     */
    public List<Knowledge> listFilesForUser(String externalUserId) {
        return repository.findByExternalUserIdAndScopeOrderByCreatedAtDesc(externalUserId, "PERSONAL");
    }

    /**
     * 获取格式化后的文件列表字符串
     *
     * @param externalUserId 用户ID
     * @return 格式化后的文件列表字符串
     */
    public String getFormattedFileListForUser(String externalUserId) {
        List<Knowledge> entries = listFilesForUser(externalUserId);
        if (entries.isEmpty()) {
            return "ℹ️ 您的知识库中还没有任何文件。";
        }

        StringBuilder sb = new StringBuilder("您的知识库文件列表：\n-------------------\n");
        for (Knowledge entry : entries) {
            sb.append(String.format("【ID: %d】 %s\n(上传于: %s)\n",
                    entry.getId(),
                    entry.getFileName(),
                    entry.getCreatedAt().format(FORMATTER)));
        }
        sb.append("-------------------\n您可以通过“删除文件 [ID]”来移除文件。");
        return sb.toString();
    }


    /**
     * 为指定用户删除一个知识库文件
     *
     * @param id             文件ID
     * @param externalUserId 用户ID
     * @return 操作结果的描述文本
     */
    public String deleteFileForUser(Long id, String externalUserId) {
        Optional<Knowledge> entryOpt = repository.findByIdAndExternalUserId(id, externalUserId);
        if (entryOpt.isEmpty()) {
            return "❌ 删除失败：未找到ID为 " + id + " 的文件，或该文件不属于您。";
        }

        repository.deleteById(id);
        logger.info("用户 [{}] 的知识库文件 [ID: {}] 已被删除。", externalUserId, id);
        return "✅ 文件 '" + entryOpt.get().getFileName() + "' (ID: " + id + ") 已从您的知识库中删除。";
    }



    /**
     * 为指定用户删除所有知识库文件
     *
     * @param externalUserId 用户ID
     * @return 操作结果的描述文本
     */
    public String deleteAllFilesForUser(String externalUserId) {
        try {
            repository.deleteByExternalUserId(externalUserId);
            logger.info("用户 [{}] 的所有知识库文件已被删除。", externalUserId);
            return "✅ 已清空您的个人知识库中的所有文件。";
        } catch (Exception e) {
            logger.error("为用户 [{}] 清空知识库时发生错误。", externalUserId, e);
            return "❌ 清空知识库失败，请稍后再试。";
        }
    }

    /**
     * 据用户ID删除其所有的知识库记录（供管理后台调用）
     * 此方法应在一个事务中被调用，且不返回用户提示信息。
     * @param externalUserId 用户的 externalUserId
     */
    @Transactional
    public void deleteKnowledgeByUserId(String externalUserId) {
        logger.info("请求删除用户 [{}] 的所有知识库记录...", externalUserId);
        repository.deleteByExternalUserId(externalUserId);
        logger.info("删除用户 [{}] 知识库记录的操作已执行。", externalUserId);
    }


    /**
     * 检索指定用户的所有知识库内容，用于增强AI问答
     *
     * @param externalUserId 用户ID
     * @return 拼接好的知识库上下文
     */
    public String retrieveKnowledgeForUser(String externalUserId) {
        List<Knowledge> entries = repository.findByExternalUserIdAndScopeOrderByCreatedAtDesc(externalUserId, "PERSONAL");
        if (entries.isEmpty()) {
            return "";
        }

        return entries.stream()
                .map(entry -> String.format("--- 来自文件: %s ---\n%s\n--- 文件结束 ---\n\n",
                        entry.getFileName(), entry.getContent()))
                .collect(Collectors.joining());
    }


    /**
     * 清空 knowledge_base 数据库表
     * 这是 deleteAllInBatch 的服务层封装
     */
    @Transactional
    public void deleteAllKnowledgeData() {
        logger.warn("正在通过服务层清空 KnowledgeBase 表...");
        repository.deleteAllInBatch();
        logger.warn("KnowledgeBase 表已通过服务层清空。");
    }

}