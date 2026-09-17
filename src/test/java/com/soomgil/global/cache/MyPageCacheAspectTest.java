package com.soomgil.global.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.global.security.CurrentUser;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class MyPageCacheAspectTest {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
    Map<String,String> entries = new HashMap<>();
    Queries target;
    Queries proxy;

    @BeforeEach void setup() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> entries.get(call.getArgument(0)));
        doAnswer(call -> { entries.put(call.getArgument(0),call.getArgument(1)); return null; })
            .when(values).set(anyString(),anyString(),any(Duration.class));
        doAnswer(call -> { entries.put(call.getArgument(0),call.getArgument(1)); return null; })
            .when(values).set(anyString(),anyString());
        target = new Queries();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new MyPageCacheAspect(redis,new ObjectMapper().findAndRegisterModules(),120));
        proxy = factory.getProxy();
        login(UUID.randomUUID());
    }
    void login(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new CurrentUser(userId,null),null,java.util.List.of()));
    }
    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        if(TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }
    @Test void cachesQueriesAndSeparatesUsersAndArguments() {
        assertThat(proxy.load(0).value()).isEqualTo(1);
        assertThat(proxy.load(0).value()).isEqualTo(1);
        assertThat(proxy.load(1).value()).isEqualTo(2);
        login(UUID.randomUUID());
        assertThat(proxy.load(0).value()).isEqualTo(3);
        verify(values,atLeastOnce()).set(anyString(),anyString(),eq(Duration.ofSeconds(120)));
    }
    @Test void reloadsAfterSuccessfulMutation() {
        proxy.load(0); proxy.change();
        assertThat(proxy.load(0).value()).isEqualTo(2);
    }
    @Test void invalidatesOnlyAfterCommitAndDoesNotInvalidateOnRollback() {
        proxy.load(0);
        TransactionSynchronizationManager.initSynchronization();
        proxy.change();
        assertThat(proxy.load(0).value()).isEqualTo(1);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        TransactionSynchronizationManager.clearSynchronization();
        assertThat(proxy.load(0).value()).isEqualTo(2);
        TransactionSynchronizationManager.initSynchronization();
        proxy.change();
        TransactionSynchronizationManager.clearSynchronization();
        assertThat(proxy.load(0).value()).isEqualTo(2);
    }
    @Test void fallsBackWhenRedisIsUnavailable() {
        when(values.get(anyString())).thenThrow(new IllegalStateException("unavailable"));
        assertThat(proxy.load(0).value()).isEqualTo(1);
        assertThat(proxy.load(0).value()).isEqualTo(2);
    }
    @Test void corruptCacheDoesNotBreakRead() {
        proxy.load(0);
        entries.replaceAll((key,value) -> key.endsWith(":generation") ? value : "broken json");
        assertThat(proxy.load(0).value()).isEqualTo(2);
    }
    @Test void failedMutationsDoNotInvalidate() {
        proxy.load(0);
        assertThatThrownBy(() -> proxy.fail()).isInstanceOf(IllegalArgumentException.class);
        assertThat(proxy.load(0).value()).isEqualTo(1);
    }
    @Test void unauthenticatedCallsNeverUseCache() {
        SecurityContextHolder.clearContext();
        proxy.load(0); proxy.load(0);
        assertThat(entries).isEmpty();
        assertThat(target.calls).isEqualTo(2);
    }
    @Test void inFlightOldReadCannotRepopulateNewGeneration() {
        target.onLoad = () -> proxy.change();
        assertThat(proxy.load(0).value()).isEqualTo(1);
        target.onLoad = null;
        assertThat(proxy.load(0).value()).isEqualTo(2);
    }
    @Test void writeFailureStillReturnsDatabaseResult() {
        doThrow(new IllegalStateException("unavailable")).when(values).set(anyString(),anyString(),any(Duration.class));
        assertThat(proxy.load(0).value()).isEqualTo(1);
    }
    @Test void resolvesConcreteReturnTypeBehindGenericHandlerInterface() {
        var factory = new AspectJProxyFactory(new GenericQueries());
        factory.addAspect(new MyPageCacheAspect(redis,new ObjectMapper(),120));
        GenericQuery<Result> handler = factory.getProxy();
        assertThat(handler.handle(0)).isEqualTo(new Result(1));
        assertThat(handler.handle(0)).isEqualTo(new Result(1));
    }
    public interface GenericQuery<T> { T handle(int page); }
    public static class GenericQueries implements GenericQuery<Result> {
        int calls;
        @MyPageCached("profile") public Result handle(int page) { return new Result(++calls); }
    }
    public record Result(int value) {}
    public static class Queries {
        int calls;
        Runnable onLoad;
        @MyPageCached("profile") public Result load(int page) { if(onLoad != null) onLoad.run(); return new Result(++calls); }
        @InvalidatesMyPageCache({"profile"}) public void change() {}
        @InvalidatesMyPageCache({"profile"}) public void fail() { throw new IllegalArgumentException(); }
    }
}
