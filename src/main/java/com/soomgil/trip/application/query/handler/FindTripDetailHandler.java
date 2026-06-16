package com.soomgil.trip.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.query.dto.FindTripDetailQuery;
import com.soomgil.trip.application.query.dto.TripAccessView;
import com.soomgil.trip.application.query.dto.TripDetailView;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FindTripDetailQuery}를 처리해 여행방 상세 정보를 조회한다.
 *
 * <p>요청 사용자가 active member인지 먼저 확인하고, 권한이 없으면 상세 정보를 읽지 않는다.
 */
@Component
public class FindTripDetailHandler implements QueryHandler<FindTripDetailQuery, TripDetailView> {

	private final TripAccessGuard accessGuard;
	private final TripQueryRepository repository;

	public FindTripDetailHandler(TripAccessGuard accessGuard, TripQueryRepository repository) {
		this.accessGuard = Objects.requireNonNull(accessGuard, "accessGuard must not be null");
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public TripDetailView handle(FindTripDetailQuery query) {
		TripAccessView access = accessGuard.requireActiveMember(query.tripId(), query.userId());
		TripReadModel trip = repository.findTrip(query.tripId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip was not found."));
		List<TripMemberView> members = repository.findTripMembers(query.tripId(), TripMemberStatus.ACTIVE)
			.stream()
			.map(TripViewMapper::toMemberView)
			.toList();
		return new TripDetailView(
			trip.id(),
			trip.title(),
			trip.displayDestination(),
			trip.status(),
			access.accessRole(),
			trip.itineraryVersion(),
			trip.createdAt(),
			trip.ownerUserId(),
			members,
			trip.retrippedFromPostId()
		);
	}
}
