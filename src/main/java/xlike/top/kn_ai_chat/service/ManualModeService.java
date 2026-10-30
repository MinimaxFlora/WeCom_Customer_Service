package xlike.top.kn_ai_chat.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ManualModeService {

    private static final String PREFIX = "manual_chat_mode:";
    private static final long TIMEOUT_MINUTES = 30;

    private final StringRedisTemplate redisTemplate;

    public ManualModeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isActive(String externalUserId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + externalUserId));
    }

    public void enter(String externalUserId) {
        redisTemplate.opsForValue().set(PREFIX + externalUserId, "true", TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    public void refresh(String externalUserId) {
        redisTemplate.expire(PREFIX + externalUserId, TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    public void exit(String externalUserId) {
        redisTemplate.delete(PREFIX + externalUserId);
    }

    public String conversationId(String openKfid, String externalUserId) {
        return (openKfid == null ? "default" : openKfid) + ":" + externalUserId;
    }
}
