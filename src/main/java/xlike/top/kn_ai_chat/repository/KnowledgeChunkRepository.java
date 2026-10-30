package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xlike.top.kn_ai_chat.domain.KnowledgeChunk;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByEnterpriseIdAndScope(String enterpriseId, String scope);

    List<KnowledgeChunk> findByKnowledgeIdOrderByChunkIndexAsc(Long knowledgeId);

    void deleteByKnowledgeId(Long knowledgeId);
}
