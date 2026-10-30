package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import xlike.top.kn_ai_chat.domain.EnterpriseProfile;
import xlike.top.kn_ai_chat.domain.Knowledge;
import xlike.top.kn_ai_chat.domain.KnowledgeChunk;
import xlike.top.kn_ai_chat.dto.KnowledgeHit;
import xlike.top.kn_ai_chat.dto.KnowledgeRequest;
import xlike.top.kn_ai_chat.dto.KnowledgeSummary;
import xlike.top.kn_ai_chat.repository.KnowledgeBaseRepository;
import xlike.top.kn_ai_chat.repository.KnowledgeChunkRepository;
import xlike.top.kn_ai_chat.service.storage.FileStorage;
import xlike.top.kn_ai_chat.utils.VectorUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class EnterpriseKnowledgeService {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseKnowledgeService.class);
    private static final String SCOPE = "ENTERPRISE";
    private static final String ENTERPRISE_ID = "default";
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "xls", "xlsx", "csv", "txt", "md", "html", "htm"
    );
    private static final Map<String, String[]> SYNONYMS = Map.of(
            "多少钱", new String[]{"价格", "报价", "费用", "售价"},
            "价格", new String[]{"多少钱", "报价", "费用", "售价"},
            "售后", new String[]{"保修", "维修", "退换", "退款"},
            "发货", new String[]{"物流", "快递", "配送", "多久"},
            "退换", new String[]{"退货", "换货", "退款"},
            "产品", new String[]{"商品", "型号", "规格"},
            "公司", new String[]{"企业", "介绍", "简介"},
            "联系", new String[]{"电话", "地址", "客服"},
            "套餐", new String[]{"价格", "收费", "多少钱", "费用", "价位", "产品"},
            "收费", new String[]{"价格", "多少钱", "费用", "价位", "报价"}
    );
    private static final Set<String> STOP_TERMS = Set.of(
            "都有", "有什", "什么", "么套", "怎么", "么收", "费的", "可以", "哪些", "这个", "那个"
    );

    private final KnowledgeBaseRepository knowledgeRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final FileStorage fileStorage;
    private final KnowledgeIndexService indexService;
    private final EmbeddingService embeddingService;
    private final EnterpriseProfileService profileService;

    public EnterpriseKnowledgeService(KnowledgeBaseRepository knowledgeRepository,
                                      KnowledgeChunkRepository chunkRepository,
                                      FileStorage fileStorage,
                                      KnowledgeIndexService indexService,
                                      EmbeddingService embeddingService,
                                      EnterpriseProfileService profileService) {
        this.knowledgeRepository = knowledgeRepository;
        this.chunkRepository = chunkRepository;
        this.fileStorage = fileStorage;
        this.indexService = indexService;
        this.embeddingService = embeddingService;
        this.profileService = profileService;
    }

    public List<KnowledgeSummary> list(String category, String status, String keyword) {
        List<Knowledge> entries = knowledgeRepository
                .findByEnterpriseIdAndScopeAndEnabledTrueOrderByCreatedAtDesc(ENTERPRISE_ID, SCOPE);
        return entries.stream()
                .filter(k -> !StringUtils.hasText(category) || category.equals(k.getCategory()))
                .filter(k -> !StringUtils.hasText(status) || status.equals(k.getStatus()))
                .filter(k -> !StringUtils.hasText(keyword)
                        || (k.getTitle() != null && k.getTitle().contains(keyword))
                        || (k.getContent() != null && k.getContent().contains(keyword)))
                .map(this::toSummary)
                .toList();
    }

    public Knowledge get(Long id) {
        return knowledgeRepository.findByIdAndScope(id, SCOPE)
                .orElseThrow(() -> new IllegalArgumentException("企业知识不存在"));
    }

    @Transactional
    public Knowledge createManual(KnowledgeRequest request) {
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("标题和内容不能为空");
        }
        Knowledge knowledge = new Knowledge();
        knowledge.setEnterpriseId(ENTERPRISE_ID);
        knowledge.setScope(SCOPE);
        knowledge.setSourceType("MANUAL");
        knowledge.setCategory(defaultCategory(request.getCategory()));
        knowledge.setTitle(request.getTitle().trim());
        knowledge.setFileName(request.getTitle().trim());
        knowledge.setContent(request.getContent().trim());
        knowledge.setStatus("PENDING");
        knowledge.setEnabled(request.getEnabled() == null || request.getEnabled());
        knowledge.setCreatedAt(LocalDateTime.now());
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledgeRepository.save(knowledge);
        indexAfterCommit(knowledge.getId());
        return knowledge;
    }

    @Transactional
    public Knowledge update(Long id, KnowledgeRequest request) {
        Knowledge knowledge = get(id);
        if (StringUtils.hasText(request.getTitle())) {
            knowledge.setTitle(request.getTitle().trim());
            knowledge.setFileName(request.getTitle().trim());
        }
        if (StringUtils.hasText(request.getCategory())) {
            knowledge.setCategory(request.getCategory());
        }
        if (request.getContent() != null) {
            knowledge.setContent(request.getContent().trim());
        }
        if (request.getEnabled() != null) {
            knowledge.setEnabled(request.getEnabled());
        }
        knowledge.setStatus("PENDING");
        knowledge.setErrorMessage(null);
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledgeRepository.save(knowledge);
        indexAfterCommit(id);
        return knowledge;
    }

    @Transactional
    public void delete(Long id) {
        Knowledge knowledge = get(id);
        chunkRepository.deleteByKnowledgeId(id);
        if (knowledge.getStoragePath() != null) {
            fileStorage.delete(knowledge.getStoragePath());
        }
        knowledgeRepository.delete(knowledge);
    }

    @Transactional
    public Knowledge upload(MultipartFile file, String category) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件不能超过 20MB");
        }
        String original = file.getOriginalFilename() == null ? "file.txt" : file.getOriginalFilename();
        original = original.replace("\\", "/");
        original = original.substring(original.lastIndexOf('/') + 1);
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 PDF/DOCX/XLS/XLSX/CSV/TXT/MD/HTML");
        }

        String path = fileStorage.store(file, "enterprise");
        Knowledge knowledge = new Knowledge();
        knowledge.setEnterpriseId(ENTERPRISE_ID);
        knowledge.setScope(SCOPE);
        knowledge.setSourceType("ADMIN_UPLOAD");
        knowledge.setCategory(defaultCategory(category));
        knowledge.setTitle(original);
        knowledge.setFileName(original);
        knowledge.setContent("");
        knowledge.setStoragePath(path);
        knowledge.setMimeType(file.getContentType());
        knowledge.setFileSize(file.getSize());
        knowledge.setStatus("PENDING");
        knowledge.setEnabled(true);
        knowledge.setCreatedAt(LocalDateTime.now());
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledgeRepository.save(knowledge);
        indexAfterCommit(knowledge.getId());
        return knowledge;
    }

    public void reindex(Long id) {
        Knowledge knowledge = get(id);
        knowledge.setStatus("PENDING");
        knowledge.setErrorMessage(null);
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledgeRepository.save(knowledge);
        indexAfterCommit(id);
    }

    private void indexAfterCommit(Long id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    indexService.indexAsync(id);
                }
            });
        } else {
            indexService.indexAsync(id);
        }
    }

    public List<KnowledgeHit> search(String query, int topK) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        List<KnowledgeChunk> chunks = chunkRepository.findByEnterpriseIdAndScope(ENTERPRISE_ID, SCOPE);
        Map<Long, Knowledge> knowledgeById = knowledgeRepository
                .findByEnterpriseIdAndScopeAndEnabledTrueOrderByCreatedAtDesc(ENTERPRISE_ID, SCOPE)
                .stream().collect(java.util.stream.Collectors.toMap(Knowledge::getId, k -> k));

        Optional<float[]> queryVector = embeddingService.embed(query);
        List<KnowledgeHit> hits = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            Knowledge knowledge = knowledgeById.get(chunk.getKnowledgeId());
            if (knowledge == null || "FAILED".equals(knowledge.getStatus())) {
                continue;
            }
            double score = 0;
            float[] chunkVector = VectorUtils.fromBytes(chunk.getEmbedding());
            if (queryVector.isPresent() && chunkVector != null) {
                score = VectorUtils.cosine(queryVector.get(), chunkVector);
            }
            if (score <= 0) {
                score = lexicalScore(query, chunk.getContent(), knowledge.getTitle());
            }
            if (score >= 0.15) {
                hits.add(new KnowledgeHit(
                        knowledge.getId(),
                        knowledge.getTitle(),
                        knowledge.getCategory(),
                        chunk.getContent(),
                        Math.min(1.0, score)
                ));
            }
        }
        // 企业资料是结构化公共知识，不依赖文档切片；企业介绍类问题必须能命中它
        if (isProfileQuestion(query)) {
            String profile = buildProfileContext();
            if (StringUtils.hasText(profile)) {
                hits.add(new KnowledgeHit(0L, "企业资料", "COMPANY", profile, 0.9));
            }
        }
        return hits.stream()
                .sorted(Comparator.comparingDouble(KnowledgeHit::score).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    private boolean isProfileQuestion(String query) {
        if (query == null) {
            return false;
        }
        String[] hints = {"公司", "企业", "干什么", "做什么", "做啥", "干嘛", "业务",
                "介绍", "简介", "行业", "服务范围", "地址", "电话", "营业", "联系", "在哪", "哪里"};
        for (String hint : hints) {
            if (query.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    public String buildContext(String query, int topK) {
        List<KnowledgeHit> hits = search(query, topK);
        if (hits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (KnowledgeHit hit : hits) {
            String block = "【资料" + index + "】分类：" + safe(hit.category()) + "；标题：" + safe(hit.title())
                    + "；相关度：" + String.format("%.2f", hit.score()) + "\n"
                    + hit.content() + "\n\n";
            if (sb.length() + block.length() > 7000) {
                break;
            }
            sb.append(block);
            index++;
        }
        return sb.toString().trim();
    }

    public String buildProfileContext() {
        EnterpriseProfile profile = profileService.get();
        StringBuilder sb = new StringBuilder();
        appendProfile(sb, "企业名称", profile.getName());
        appendProfile(sb, "企业简介", profile.getIntro());
        appendProfile(sb, "所属行业", profile.getIndustry());
        appendProfile(sb, "企业地址", profile.getAddress());
        appendProfile(sb, "联系电话", profile.getPhone());
        appendProfile(sb, "官方网站", profile.getWebsite());
        appendProfile(sb, "营业时间", profile.getBusinessHours());
        appendProfile(sb, "服务范围", profile.getServiceScope());
        return sb.toString().trim();
    }

    private void appendProfile(StringBuilder sb, String label, String value) {
        if (StringUtils.hasText(value)) {
            sb.append(label).append("：").append(value.trim()).append("\n");
        }
    }

    private double lexicalScore(String query, String content, String title) {
        if (content == null) {
            return 0;
        }
        Set<String> terms = terms(query);
        if (terms.isEmpty()) {
            return 0;
        }
        String lowerContent = content.toLowerCase(Locale.ROOT);
        String lowerTitle = title == null ? "" : title.toLowerCase(Locale.ROOT);
        int matched = 0;
        for (String term : terms) {
            if (lowerContent.contains(term) || lowerTitle.contains(term)) {
                matched++;
            }
        }
        double score = (double) matched / terms.size();
        if (lowerTitle.contains(query.toLowerCase(Locale.ROOT))) {
            score += 0.2;
        } else {
            for (String term : terms) {
                if (lowerTitle.contains(term)) {
                    score += 0.25;
                    break;
                }
            }
        }
        return Math.min(1.0, score);
    }

    private Set<String> terms(String query) {
        String normalized = query.toLowerCase(Locale.ROOT).replaceAll("[\\p{P}\\p{S}\\s]+", " ");
        Set<String> terms = new LinkedHashSet<>();
        for (String part : normalized.split(" ")) {
            if (part.isBlank()) {
                continue;
            }
            if (part.matches(".*[\\u4e00-\\u9fa5].*")) {
                if (part.length() <= 2) {
                    if (!STOP_TERMS.contains(part)) {
                        terms.add(part);
                    }
                } else {
                    for (int i = 0; i < part.length() - 1; i++) {
                        String term = part.substring(i, i + 2);
                        if (!STOP_TERMS.contains(term)) {
                            terms.add(term);
                        }
                    }
                }
                for (Map.Entry<String, String[]> entry : SYNONYMS.entrySet()) {
                    if (part.contains(entry.getKey())) {
                        terms.addAll(List.of(entry.getValue()));
                    }
                }
            } else {
                terms.add(part);
            }
        }
        return terms;
    }

    private String defaultCategory(String category) {
        return StringUtils.hasText(category) ? category : "CUSTOM";
    }

    private KnowledgeSummary toSummary(Knowledge k) {
        return new KnowledgeSummary(k.getId(), k.getTitle(), k.getCategory(), k.getSourceType(),
                k.getStatus(), k.getFileName(), k.getFileSize(), k.getErrorMessage(), k.getUpdatedAt());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
