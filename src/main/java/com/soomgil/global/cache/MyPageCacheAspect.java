package com.soomgil.global.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.soomgil.global.security.CurrentUser;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.DigestUtils;

/**
 * 마이페이지의 반복 조회를 Redis에서 반환하는 읽기 캐시.
 *
 * <p>키에는 인증 사용자, 메서드, 인자를 포함하고 JSON은 메서드의 명시적 반환 타입으로만 읽는다.
 * 변경은 영역별 세대 키를 교체해 무효화하므로 변경 전 시작된 조회가 늦게 완료되어도
 * 오래된 응답이 새 세대에 저장되지 않는다. Redis 장애 시 원래 조회를 수행한다.
 * 관계/작성자 변경이 다른 사용자 화면에도 영향을 주므로 영역 단위로 보수적으로 무효화한다.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class MyPageCacheAspect {
    private static final Logger log = LoggerFactory.getLogger(MyPageCacheAspect.class);
    private static final String PREFIX = "soomgil:mypage:v1:";
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;

    /** TTL은 초 단위이며 Redis 실패가 사용자 요청을 실패시키지 않도록 처리한다. */
    public MyPageCacheAspect(StringRedisTemplate redis, ObjectMapper mapper,
        @Value("${soomgil.mypage-cache.ttl-seconds:120}") long ttlSeconds) {
        this.redis = redis;
        this.mapper = mapper.copy()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
    }

    /** 인증된 조회만 캐시하고, 오류 및 null 응답은 저장하지 않는다. */
    @Around("@annotation(cached)")
    public Object read(ProceedingJoinPoint call, MyPageCached cached) throws Throwable {
        if (TransactionSynchronizationManager.isActualTransactionActive()
            && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) return call.proceed();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof CurrentUser user)) return call.proceed();
        Method method = AopUtils.getMostSpecificMethod(((MethodSignature)call.getSignature()).getMethod(),call.getTarget().getClass());
        String key;
        try {
            String generation = redis.opsForValue().get(PREFIX + cached.value() + ":generation");
            String arguments = method.toGenericString() + mapper.writeValueAsString(call.getArgs());
            key = PREFIX + cached.value() + ":" + (generation == null ? "0" : generation)
                + ":" + user.userId() + ":" + DigestUtils.md5DigestAsHex(arguments.getBytes(StandardCharsets.UTF_8));
            String json = redis.opsForValue().get(key);
            if (json != null) return mapper.readValue(json, mapper.constructType(method.getGenericReturnType()));
        } catch (Exception error) {
            log.debug("My page cache read unavailable: {}", error.getClass().getSimpleName());
            return call.proceed();
        }
        Object result = call.proceed();
        if (result != null) {
            try { redis.opsForValue().set(key, mapper.writeValueAsString(result), ttl); }
            catch (Exception error) { log.debug("My page cache write unavailable: {}", error.getClass().getSimpleName()); }
        }
        return result;
    }

    /** 트랜잭션이 있으면 커밋 이후, 없으면 성공 반환 직후 조회 영역을 무효화한다. */
    @Around("@annotation(invalidation)")
    public Object change(ProceedingJoinPoint call, InvalidatesMyPageCache invalidation) throws Throwable {
        Object result = call.proceed();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { invalidate(invalidation.value()); }
            });
        } else invalidate(invalidation.value());
        return result;
    }

    private void invalidate(String[] groups) {
        for (String group : groups) {
            try { redis.opsForValue().set(PREFIX + group + ":generation", UUID.randomUUID().toString()); }
            catch (Exception error) {
                // 조회 TTL이 장애 중 남은 캐시의 최대 수명을 제한한다.
                log.warn("My page cache invalidation unavailable for {}: {}", group, error.getClass().getSimpleName());
            }
        }
    }
}
