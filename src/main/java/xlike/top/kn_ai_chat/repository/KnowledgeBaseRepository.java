package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional; // 导入 @Transactional
import xlike.top.kn_ai_chat.domain.Knowledge;

import java.util.List;
import java.util.Optional;

/**
 * @author xlike
 */
public interface KnowledgeBaseRepository extends JpaRepository<Knowledge, Long> {

    List<Knowledge> findByExternalUserId(String externalUserId);

    Optional<Knowledge> findByIdAndExternalUserId(Long id, String externalUserId);

    List<Knowledge> findByExternalUserIdAndScopeOrderByCreatedAtDesc(String externalUserId, String scope);

    List<Knowledge> findByScopeAndEnabledTrueOrderByCreatedAtDesc(String scope);

    List<Knowledge> findByScopeAndCategoryAndEnabledTrueOrderByCreatedAtDesc(String scope, String category);

    List<Knowledge> findByEnterpriseIdAndScopeAndEnabledTrueOrderByCreatedAtDesc(String enterpriseId, String scope);

    Optional<Knowledge> findByIdAndScope(Long id, String scope);

    /**
     * 根据用户ID删除其所有知识库条目
     * @param externalUserId 用户的 external_userid
     */
    @Transactional
    void deleteByExternalUserId(String externalUserId);
}