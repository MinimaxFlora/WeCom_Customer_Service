package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import xlike.top.kn_ai_chat.dispatcher.MessageDispatcher;
import xlike.top.kn_ai_chat.domain.Knowledge;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.dto.KnowledgeHit;
import xlike.top.kn_ai_chat.dto.KnowledgeRequest;
import xlike.top.kn_ai_chat.dto.KnowledgeSummary;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.AiService;
import xlike.top.kn_ai_chat.service.EnterpriseKnowledgeService;
import xlike.top.kn_ai_chat.service.EnterpriseProfileService;
import xlike.top.kn_ai_chat.service.WeChatKfAccountService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class EnterpriseKnowledgeController {

    private final EnterpriseKnowledgeService knowledgeService;
    private final AiService aiService;
    private final EnterpriseProfileService profileService;
    private final MessageDispatcher messageDispatcher;
    private final WeChatKfAccountService kfAccountService;

    public EnterpriseKnowledgeController(EnterpriseKnowledgeService knowledgeService,
                                         AiService aiService,
                                         EnterpriseProfileService profileService,
                                         MessageDispatcher messageDispatcher,
                                         WeChatKfAccountService kfAccountService) {
        this.knowledgeService = knowledgeService;
        this.aiService = aiService;
        this.profileService = profileService;
        this.messageDispatcher = messageDispatcher;
        this.kfAccountService = kfAccountService;
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    @GetMapping("/knowledge")
    public String page(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }
        return "knowledge";
    }

    @GetMapping("/api/knowledge")
    @ResponseBody
    public ResponseEntity<List<KnowledgeSummary>> list(@RequestParam(required = false) String category,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String keyword,
                                                       HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(knowledgeService.list(category, status, keyword));
    }

    @GetMapping("/api/knowledge/{id}")
    @ResponseBody
    public ResponseEntity<Knowledge> get(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(knowledgeService.get(id));
    }

    @PostMapping("/api/knowledge")
    @ResponseBody
    public ResponseEntity<Knowledge> create(@RequestBody KnowledgeRequest request, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(knowledgeService.createManual(request));
    }

    @PutMapping("/api/knowledge/{id}")
    @ResponseBody
    public ResponseEntity<Knowledge> update(@PathVariable Long id,
                                            @RequestBody KnowledgeRequest request,
                                            HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(knowledgeService.update(id, request));
    }

    @DeleteMapping("/api/knowledge/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        knowledgeService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/knowledge/upload")
    @ResponseBody
    public ResponseEntity<Knowledge> upload(@RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String category,
                                            HttpSession session) throws Exception {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(knowledgeService.upload(file, category));
    }

    @PostMapping("/api/knowledge/{id}/reindex")
    @ResponseBody
    public ResponseEntity<Void> reindex(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        knowledgeService.reindex(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/knowledge/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> test(@RequestBody Map<String, Object> request,
                                                    HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        String query = String.valueOf(request.getOrDefault("query", ""));
        int topK = request.get("topK") instanceof Number n ? n.intValue() : 5;
        List<KnowledgeHit> hits = knowledgeService.search(query, topK);
        String context = knowledgeService.buildContext(query, topK);
        return ResponseEntity.ok(Map.of(
                "hits", hits,
                "context", context
        ));
    }

    @PostMapping("/api/knowledge/answer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> answer(@RequestBody Map<String, Object> request,
                                                      HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        String query = String.valueOf(request.getOrDefault("query", ""));
        String context = knowledgeService.buildContext(query, 5);
        String answer = aiService.answerEnterpriseQuestion(query, context, profileService.get());
        return ResponseEntity.ok(Map.of("answer", answer, "context", context));
    }

    /**
     * 管理后台模拟一条普通客户消息，走完整 Handler 链（不发送微信消息），用于验收自动 RAG。
     */
    @PostMapping("/api/knowledge/simulate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> simulate(@RequestBody Map<String, Object> request,
                                                        HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        String userId = String.valueOf(request.getOrDefault("userId", "test-user"));
        String content = String.valueOf(request.getOrDefault("content", ""));
        String openKfid = kfAccountService.getOpenKfid();
        if (openKfid == null || openKfid.isBlank()) {
            openKfid = "default";
        }
        MessageLog message = new MessageLog();
        message.setFromUser(userId);
        message.setToUser(openKfid);
        message.setMsgType("text");
        message.setContent(content);
        message.setTimestamp(java.time.LocalDateTime.now());
        Reply reply = messageDispatcher.dispatch(userId, openKfid, content, List.of(message)).orElse(null);
        String text = reply instanceof TextReply textReply ? textReply.content()
                : reply == null ? "" : reply.toString();
        return ResponseEntity.ok(Map.of("reply", text, "type",
                reply == null ? "none" : reply.getClass().getSimpleName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
