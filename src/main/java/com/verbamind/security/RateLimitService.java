package com.verbamind.security;

import com.verbamind.exception.ApiException;
import com.verbamind.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public void checkRateLimit(String bucket, String identifier, int maxAttempts, Duration window) {
        String key = "ratelimit:" + bucket + ":" + identifier;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }

        if (count != null && count > maxAttempts) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Too many attempts. Please try again in a few minutes."
            );
        }
    }
}
