package com.soomgil.trip.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.dto.ListTripInvitesQuery;
import com.soomgil.trip.application.query.dto.TripInviteView;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ListTripInvitesQuery}를 처리해 여행방 초대 목록을 조회한다.
 *
 * <p>초대 code는 owner에게만 노출한다.
 */
@Component
public class ListTripInvitesHandler implements QueryHandler<ListTripInvitesQuery, List<TripInviteView>> {

	private final TripAccessGuard accessGuard;
	private final TripQueryRepository repository;

	public ListTripInvitesHandler(TripAccessGuard accessGuard, TripQueryRepository repository) {
		this.accessGuard = Objects.requireNonNull(accessGuard, "accessGuard must not be null");
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public List<TripInviteView> handle(ListTripInvitesQuery query) {
		accessGuard.requireOwner(query.tripId(), query.actorUserId());
		return repository.findTripInvites(query.tripId(), query.status())
			.stream()
			.map(invite -> new TripInviteView(
				invite.id(),
				invite.tripId(),
				invite.inviteCode(),
				invite.inviteeUserId(),
				invite.status(),
				invite.expiresAt(),
				invite.createdAt()
			))
			.toList();
	}
}
