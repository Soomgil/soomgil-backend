package com.soomgil.trip.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.dto.FindTripAccessQuery;
import com.soomgil.trip.application.query.dto.TripAccessView;
import com.soomgil.trip.domain.policy.TripAccessPolicy;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FindTripAccessQuery}를 처리해 여행방 접근 권한을 계산한다.
 *
 * <p>읽기 handler이며 side effect가 없다. 여행방이 없거나 active 상태가 아니면
 * 예외 대신 접근 불가 view를 반환해 호출 모듈이 자신의 실패 정책으로 변환할 수 있게 한다.
 */
@Component
public class FindTripAccessHandler implements QueryHandler<FindTripAccessQuery, TripAccessView> {

	private final TripQueryRepository repository;

	public FindTripAccessHandler(TripQueryRepository repository) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public TripAccessView handle(FindTripAccessQuery query) {
		Objects.requireNonNull(query.tripId(), "tripId must not be null");
		Objects.requireNonNull(query.userId(), "userId must not be null");
		TripAccessSnapshot snapshot = repository.findTripAccess(query.tripId(), query.userId()).orElse(null);
		return TripAccessPolicy.evaluate(query.tripId(), query.userId(), snapshot);
	}
}
