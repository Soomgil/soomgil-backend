package com.soomgil.trip.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.dto.TripMemberView;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ListTripMembersQuery}를 처리해 여행방 멤버 목록을 조회한다.
 *
 * <p>여행방 멤버 목록은 active member에게만 노출한다.
 */
@Component
public class ListTripMembersHandler implements QueryHandler<ListTripMembersQuery, List<TripMemberView>> {

	private final TripAccessGuard accessGuard;
	private final TripQueryRepository repository;

	public ListTripMembersHandler(TripAccessGuard accessGuard, TripQueryRepository repository) {
		this.accessGuard = Objects.requireNonNull(accessGuard, "accessGuard must not be null");
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public List<TripMemberView> handle(ListTripMembersQuery query) {
		accessGuard.requireActiveMember(query.tripId(), query.userId());
		return repository.findTripMembers(query.tripId(), query.status())
			.stream()
			.map(TripViewMapper::toMemberView)
			.toList();
	}
}
