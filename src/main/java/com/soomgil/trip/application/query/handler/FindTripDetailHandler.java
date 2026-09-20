package com.soomgil.trip.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.geo.application.query.dto.FindLegalRegionsByCodesQuery;
import com.soomgil.geo.application.query.dto.LegalRegionView;
import com.soomgil.geo.application.query.handler.FindLegalRegionsByCodesHandler;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.query.dto.FindTripDetailQuery;
import com.soomgil.trip.application.query.dto.TripAccessView;
import com.soomgil.trip.application.query.dto.TripDetailView;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.UUID;
import java.util.Map;
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
	private final FindLegalRegionsByCodesHandler legalRegionsHandler;

	public FindTripDetailHandler(
		TripAccessGuard accessGuard,
		TripQueryRepository repository,
		FindLegalRegionsByCodesHandler legalRegionsHandler
	) {
		this.accessGuard = Objects.requireNonNull(accessGuard, "accessGuard must not be null");
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.legalRegionsHandler = Objects.requireNonNull(legalRegionsHandler, "legalRegionsHandler must not be null");
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
			trip.startDate(),
			trip.endDate(),
			trip.createdAt(),
			trip.ownerUserId(),
			members,
			trip.retrippedFromPostId(),
			regionsOf(query.tripId())
		);
	}
	/**
	 * 여행방에 등록한 순서대로 지역 이름을 채운다. trip DB에는 코드만 있으므로 geo의 공개 query로 이름을 얻는다.
	 * geo에 없는 코드는 결과에서 빠진다.
	 */
	private List<LegalRegionView> regionsOf(UUID tripId) {
		List<String> codes = repository.findTripRegionCodes(tripId);
		if (codes.isEmpty()) {
			return List.of();
		}
		Map<String, LegalRegionView> byCode = legalRegionsHandler
			.handle(new FindLegalRegionsByCodesQuery(codes))
			.stream()
			.collect(Collectors.toMap(LegalRegionView::code, Function.identity(), (first, second) -> first));
		return codes.stream()
			.map(byCode::get)
			.filter(Objects::nonNull)
			.toList();
	}
}
