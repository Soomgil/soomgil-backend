package com.soomgil.planning.application.config;

import com.soomgil.planning.application.service.NoOpPlanningEventBroadcaster;
import com.soomgil.planning.application.service.NoOpTripMemberAccessChecker;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
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

	/**
	 * trip 모듈 구현 전까지 사용할 stub {@link TripMemberAccessChecker}.
	 *
	 * @return 아무 검증도 하지 않는 checker
	 */
	@Bean
	@ConditionalOnMissingBean(TripMemberAccessChecker.class)
	TripMemberAccessChecker tripMemberAccessChecker() {
		return new NoOpTripMemberAccessChecker();
	}

	/**
	 * realtime 인프라 구현 전까지 사용할 stub {@link PlanningEventBroadcaster}.
	 *
	 * @return 이벤트를 무시하는 broadcaster
	 */
	@Bean
	@ConditionalOnMissingBean(PlanningEventBroadcaster.class)
	PlanningEventBroadcaster planningEventBroadcaster() {
		return new NoOpPlanningEventBroadcaster();
	}
}

