package com.pro.messageservice.service;

import com.pro.messageservice.model.SenderType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String TARGET_RATE_LIMIT_KEY = "otp:ratelimit:target:";
    private static final String IP_RATE_LIMIT_KEY = "otp:ratelimit:ip:";
    private static final int MAX_TARGET_REQUESTS = 10;
    private static final int MAX_IP_REQUESTS = 10;
    private static final long TARGET_TTL_SECONDS = 86400L; // 1 день
    private static final long IP_TTL_SECONDS = 3600L;      // 1 час

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local max = tonumber(ARGV[1])
            local ttl = tonumber(ARGV[2])
            local current = redis.call('GET', key)
            if current and tonumber(current) >= max then
                return 0
            end
            local count = redis.call('INCR', key)
            if count == 1 then
                redis.call('EXPIRE', key, ttl)
            end
            return 1
            """;

    public boolean checkTargetRateLimit(SenderType channel, String target) {
        String key = TARGET_RATE_LIMIT_KEY + channel.getId() + ":" + target;
        return executeRateLimitScript(key, MAX_TARGET_REQUESTS, TARGET_TTL_SECONDS);
    }

    public boolean checkIpRateLimit(String ip) {
        String key = IP_RATE_LIMIT_KEY + ip;
        return executeRateLimitScript(key, MAX_IP_REQUESTS, IP_TTL_SECONDS);
    }

    private boolean executeRateLimitScript(String key, int max, long ttl) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
        Long result = redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(max),
                String.valueOf(ttl)
        );
        return Long.valueOf(1L).equals(result);
    }
}
