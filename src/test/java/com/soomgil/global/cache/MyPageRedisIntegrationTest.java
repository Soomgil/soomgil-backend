package com.soomgil.global.cache;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.global.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 격리된 Redis에서 JSON 재조회 및 변경 후 캐시 교체를 검증한다. */
@EnabledIfEnvironmentVariable(named = "REDIS_TEST_PORT", matches = "[0-9]+")
class MyPageRedisIntegrationTest {
    @Test void roundTripsRecordsAndExpiresInvalidatedGenerations() {
        var connection = new LettuceConnectionFactory("127.0.0.1",Integer.parseInt(System.getenv("REDIS_TEST_PORT")));
        connection.afterPropertiesSet(); connection.start();
        try {
            var redis = new StringRedisTemplate(connection);
            var mapper = new ObjectMapper().findAndRegisterModules();
            var target = new Queries();
            var factory = new AspectJProxyFactory(target);
            factory.addAspect(new MyPageCacheAspect(redis,mapper,120));
            Queries proxy = factory.getProxy();
            UUID userId = UUID.randomUUID();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(userId,null),null,List.of()));
            Snapshot first = proxy.load();
            assertThat(proxy.load()).isEqualTo(first);
            assertThat(target.calls).isEqualTo(1);
            var keys = redis.keys("soomgil:mypage:v1:profile:*:" + userId + ":*");
            assertThat(keys).hasSize(1);
            assertThat(redis.getExpire(keys.iterator().next())).isBetween(1L,120L);
            proxy.change();
            assertThat(proxy.load().revision()).isEqualTo(2);
        } finally { SecurityContextHolder.clearContext(); connection.destroy(); }
    }
    public record Snapshot(UUID id, OffsetDateTime createdAt, List<String> titles, int revision) {}
    public static class Queries {
        int calls;
        @MyPageCached("profile") public Snapshot load() { return new Snapshot(UUID.randomUUID(),OffsetDateTime.now(),List.of("여행"),++calls); }
        @InvalidatesMyPageCache({"profile"}) public void change() {}
    }
}
