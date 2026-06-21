package com.soomgil.planning.application.config;

import org.springframework.context.annotation.Configuration;

/**
 * planning 모듈의 fallback bean을 등록한다.
 *
 * <p>여행방(trip) 모듈이 아직 구현되지 않았으므로 {@link TripMemberAccessChecker}의 stub을,
 * {@code global/realtime} 인프라가 구축되지 않았으므로 {@link PlanningEventBroadcaster}의 stub을
 * 각각 제공한다. 두 모듈 모두 실제 구현체를 bean으로 등록하면
 * {@code @ConditionalOnMissingBean}에 의해 stub은 자동으로 비활성화된다.
 */
@Configuration
public class PlanningModuleConfig {
}
