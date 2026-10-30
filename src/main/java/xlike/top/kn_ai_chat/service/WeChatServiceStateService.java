package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信客服会话状态接口封装。
 * service_state: 0 未处理，1 智能助手接待，2 待接入池，3 人工接待，4 已结束。
 */
@Service
public class WeChatServiceStateService {

    private static final Logger logger = LoggerFactory.getLogger(WeChatServiceStateService.class);
    private final RestTemplate restTemplate;
    private final AccessTokenManager accessTokenManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeChatServiceStateService(RestTemplate restTemplate,
                                     AccessTokenManager accessTokenManager) {
        this.restTemplate = restTemplate;
        this.accessTokenManager = accessTokenManager;
    }

    public record StateResult(boolean success, int errcode, String errmsg) {
    }

    /**
     * 把会话切回智能助手接待，让 AI 重新接管。
     */
    public StateResult backToAi(String externalUserId, String openKfid) {
        if (openKfid == null || openKfid.isBlank()) {
            return new StateResult(false, -1, "缺少客服账号 open_kfid");
        }
        return transState(externalUserId, openKfid, 1);
    }

    public StateResult endSession(String externalUserId, String openKfid) {
        if (openKfid == null || openKfid.isBlank()) {
            return new StateResult(false, -1, "缺少客服账号 open_kfid");
        }
        return transState(externalUserId, openKfid, 4);
    }

    public StateResult transState(String externalUserId, String openKfid, int state) {
        try {
            String accessToken = accessTokenManager.getAccessToken();
            String url = "https://qyapi.weixin.qq.com/cgi-bin/kf/service_state/trans?access_token=" + accessToken;
            Map<String, Object> body = new HashMap<>();
            body.put("open_kfid", openKfid);
            body.put("external_userid", externalUserId);
            body.put("service_state", state);
            String response = restTemplate.postForObject(url, body, String.class);
            JsonNode root = objectMapper.readTree(response);
            int errcode = root.path("errcode").asInt(-1);
            String errmsg = root.path("errmsg").asText("");
            if (errcode == 0) {
                logger.info("微信客服会话状态已切换: user={}, open_kfid={}, state={}", externalUserId, openKfid, state);
                return new StateResult(true, 0, "ok");
            }
            logger.warn("切换微信客服会话状态失败: state={}, errcode={}, errmsg={}", state, errcode, errmsg);
            return new StateResult(false, errcode, errmsg);
        } catch (Exception e) {
            logger.warn("调用微信客服会话状态接口异常: {}", e.getMessage());
            return new StateResult(false, -1, e.getMessage());
        }
    }
}
