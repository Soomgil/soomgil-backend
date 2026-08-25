package com.soomgil.trip.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.dto.ListTripRegionCodesQuery;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ListTripRegionCodesQuery}를 처리해 여행방의 법정동 코드를 조회한다.
 *
 * <p>active member에게만 노출한다. 지역이 등록되어 있지 않으면 빈 목록을 반환하며,
 * 호출자는 이 경우 대표 목적지 문자열 같은 대체 검색 기준을 사용해야 한다.
 */
@Component
public class ListTripRegionCodesHandler implements QueryHandler<ListTripRegionCodesQuery, List<String>> {

	private final TripAccessGuard accessGuard;
	private final TripQueryRepository repository;

	public ListTripRegionCodesHandler(TripAccessGuard accessGuard, TripQueryRepository repository) {
		this.accessGuard = Objects.requireNonNull(accessGuard, "accessGuard must not be null");
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> handle(ListTripRegionCodesQuery query) {
		accessGuard.requireActiveMember(query.tripId(), query.userId());
		return repository.findTripRegionCodes(query.tripId());
	}
}
