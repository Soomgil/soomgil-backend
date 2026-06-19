package com.soomgil.community.application.config;

import com.soomgil.community.application.service.NoOpTripSnapshotChecker;
import com.soomgil.community.application.service.TripSnapshotChecker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 커뮤니티 모듈의 fallback bean을 등록한다.
 *
 * <p>여행방(trip) 모듈이 아직 구현되지 않았으므로 {@link TripSnapshotChecker}의 stub을
 * 제공한다. trip 모듈에서 {@link TripSnapshotChecker} 구현체를 bean으로 등록하면
 * {@code @ConditionalOnMissingBean}에 의해 stub은 자동으로 비활성화된다.
 */
@Configuration
public class CommunityModuleConfig {

	/**
	 * trip 모듈 구현 전까지 사용할 stub {@link TripSnapshotChecker}.
	 *
	 * @return 빈 snapshot을 반환하는 checker
	 */
	@Bean
	@ConditionalOnMissingBean(TripSnapshotChecker.class)
	TripSnapshotChecker tripSnapshotChecker() {
		return new NoOpTripSnapshotChecker();
	}
}
