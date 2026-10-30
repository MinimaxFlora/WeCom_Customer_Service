package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author xlike
 */
@Service
public class AccessTokenManager {

    private static final Logger logger = LoggerFactory.getLogger(AccessTokenManager.class);
    private static final String ACCESS_TOKEN_KEY_PREFIX = "wechat:access_token:";

    @Value("${wechat.corp-id}")
    private String corpId;

    @Value("${wechat.secret}")
    private String secret;

    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantLock lock = new ReentrantLock();

    public AccessTokenManager(RedisTemplate<String, String> redisTemplate, RestTemplate restTemplate) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
    }

    public String getAccessToken() {
        String key = ACCESS_TOKEN_KEY_PREFIX + this.corpId;
        String accessToken = redisTemplate.opsForValue().get(key);

        if (accessToken != null && !accessToken.isEmpty()) {
            return accessToken;
        }

        lock.lock();
        try {
            accessToken = redisTemplate.opsForValue().get(key);
            if (accessToken != null && !accessToken.isEmpty()) {
                return accessToken;
            }

            logger.info("缓存中无 access_token，正在从微信API获取...");
            String url = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=" + this.corpId + "&corpsecret=" + this.secret;
            String response = restTemplate.getForObject(url, String.class);
            logger.info("请求微信 Access Token 完成");

            JsonNode root = objectMapper.readTree(response);
            if (root.path("errcode").asInt() == 0) {
                String newAccessToken = root.get("access_token").asText();
                long expiresIn = root.get("expires_in").asLong();

                redisTemplate.opsForValue().set(key, newAccessToken, expiresIn - 300, TimeUnit.SECONDS);
                return newAccessToken;
            } else {
                String errorMsg = root.path("errmsg").asText("未知错误");
                throw new RuntimeException("获取 Access Token 失败: " + errorMsg);
            }
        } catch (Exception e) {
            throw new RuntimeException("获取或解析 Access Token 时发生异常", e);
        } finally {
            lock.unlock();
        }
    }
}