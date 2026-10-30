package xlike.top.kn_ai_chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xlike.top.kn_ai_chat.domain.AiConfig;
import xlike.top.kn_ai_chat.domain.KeywordConfig;
import xlike.top.kn_ai_chat.dto.AiConfigView;
import xlike.top.kn_ai_chat.service.UserConfigService;

import java.util.List;
import java.util.Map;

/**
 * @author xlike
 */
@RestController
@RequestMapping("/api/config")
public class UserConfigController {

    private final UserConfigService userConfigService;

    public UserConfigController(UserConfigService userConfigService) {
        this.userConfigService = userConfigService;
    }

    @GetMapping("/ai/{externalUserId}")
    public ResponseEntity<AiConfigView> getAiConfig(@PathVariable String externalUserId) {
        AiConfig config = userConfigService.getAiConfig(externalUserId);
        return ResponseEntity.ok(AiConfigView.from(config));
    }

    @PostMapping("/ai")
    public ResponseEntity<AiConfig> saveAiConfig(@RequestBody AiConfig aiConfig) {
        AiConfig savedConfig = userConfigService.saveOrUpdateAiConfig(aiConfig);
        return ResponseEntity.ok(savedConfig);
    }

    @GetMapping("/keywords/{externalUserId}")
    public ResponseEntity<Map<String, List<String>>> getAllKeywords(@PathVariable String externalUserId) {
        Map<String, List<String>> keywords = userConfigService.getAllKeywords(externalUserId);
        return ResponseEntity.ok(keywords);
    }

    @PostMapping("/keywords")
    public ResponseEntity<KeywordConfig> saveKeywordConfig(@RequestParam String externalUserId,
                                                         @RequestParam String handlerName,
                                                         @RequestBody List<String> keywords) {
        KeywordConfig savedConfig = userConfigService.saveOrUpdateKeywordConfig(externalUserId, handlerName, keywords);
        return ResponseEntity.ok(savedConfig);
    }
}