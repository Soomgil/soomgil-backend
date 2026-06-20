package com.soomgil.trip.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.application.query.dto.ListMyTripsQuery;
import com.soomgil.trip.application.query.dto.PagedTripSummaryView;
import com.soomgil.trip.application.query.dto.TripSummaryView;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ListMyTripsQuery}를 처리해 현재 사용자의 여행방 목록을 조회한다.
 *
 * <p>목록은 요청 사용자가 ACTIVE member인 여행방으로 제한한다.
 */
@Component
public class ListMyTripsHandler implements QueryHandler<ListMyTripsQuery, PagedTripSummaryView> {

	private static final int MAX_PAGE_SIZE = 100;

	private final TripQueryRepository repository;

	public ListMyTripsHandler(TripQueryRepository repository) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public PagedTripSummaryView handle(ListMyTripsQuery query) {
		if (query.page() < 0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Page must be greater than or equal to 0.");
		}
		if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Size must be between 1 and 100.");
		}

		TripSummaryPage result = repository.findMyTrips(
			query.userId(),
			query.status(),
			query.role(),
			query.page(),
			query.size(),
			query.sort()
		);
		List<TripSummaryView> items = result.items()
			.stream()
			.map(trip -> TripViewMapper.toSummaryView(trip, query.userId()))
			.toList();
		int totalPages = (int) Math.ceil((double) result.totalElements() / query.size());
		return new PagedTripSummaryView(
			items,
			query.page(),
			query.size(),
			result.totalElements(),
			totalPages,
			query.sort()
		);
	}
}
