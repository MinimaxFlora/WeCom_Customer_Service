package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xlike.top.kn_ai_chat.domain.Knowledge;
import xlike.top.kn_ai_chat.domain.KnowledgeChunk;
import xlike.top.kn_ai_chat.repository.KnowledgeBaseRepository;
import xlike.top.kn_ai_chat.repository.KnowledgeChunkRepository;
import xlike.top.kn_ai_chat.service.storage.FileStorage;
import xlike.top.kn_ai_chat.utils.FileContentReader;
import xlike.top.kn_ai_chat.utils.TextChunker;
import xlike.top.kn_ai_chat.utils.VectorUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class KnowledgeIndexService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeIndexService.class);
    private static final int MAX_CHARS = 600;
    private static final int OVERLAP = 80;

    private final KnowledgeBaseRepository knowledgeRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final FileStorage fileStorage;
    private final EmbeddingService embeddingService;

    public KnowledgeIndexService(KnowledgeBaseRepository knowledgeRepository,
                                 KnowledgeChunkRepository chunkRepository,
                                 FileStorage fileStorage,
                                 EmbeddingService embeddingService) {
        this.knowledgeRepository = knowledgeRepository;
        this.chunkRepository = chunkRepository;
        this.fileStorage = fileStorage;
        this.embeddingService = embeddingService;
    }

    @Async("knowledgeIndexExecutor")
    @Transactional
    public void indexAsync(Long knowledgeId) {
        Optional<Knowledge> optional = knowledgeRepository.findById(knowledgeId);
        if (optional.isEmpty()) {
            return;
        }
        Knowledge knowledge = optional.get();
        knowledge.setStatus("PROCESSING");
        knowledge.setErrorMessage(null);
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledgeRepository.save(knowledge);

        try {
            String text = knowledge.getContent();
            if ((text == null || text.isBlank()) && knowledge.getStoragePath() != null) {
                Path path = fileStorage.resolve(knowledge.getStoragePath());
                text = FileContentReader.readFileContent(path.toFile());
                knowledge.setContent(text);
            }
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("文档内容为空");
            }

            List<String> chunks = TextChunker.chunk(text, MAX_CHARS, OVERLAP);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("没有可索引的文本片段");
            }

            chunkRepository.deleteByKnowledgeId(knowledgeId);
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setKnowledgeId(knowledgeId);
                chunk.setEnterpriseId(knowledge.getEnterpriseId());
                chunk.setScope(knowledge.getScope());
                chunk.setChunkIndex(i);
                chunk.setContent(chunkText);
                chunk.setTokenCount(chunkText.length());
                chunk.setCreatedAt(LocalDateTime.now());
                embeddingService.embed(chunkText).ifPresent(vector -> chunk.setEmbedding(VectorUtils.toBytes(vector)));
                chunkRepository.save(chunk);
            }

            knowledge.setStatus("READY");
            knowledge.setUpdatedAt(LocalDateTime.now());
            knowledgeRepository.save(knowledge);
            logger.info("企业知识索引完成: id={}, chunks={}", knowledgeId, chunks.size());
        } catch (Exception e) {
            logger.warn("企业知识索引失败: id={}, error={}", knowledgeId, e.getMessage());
            Knowledge current = knowledgeRepository.findById(knowledgeId).orElse(knowledge);
            current.setStatus("FAILED");
            current.setErrorMessage(truncate(e.getMessage(), 1800));
            current.setUpdatedAt(LocalDateTime.now());
            knowledgeRepository.save(current);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
