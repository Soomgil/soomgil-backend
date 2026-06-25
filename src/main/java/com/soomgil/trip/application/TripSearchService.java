package com.soomgil.trip.application;

import com.soomgil.trip.api.dto.TripAccessRole;
import com.soomgil.trip.api.dto.TripStatus;
import com.soomgil.trip.api.dto.TripSummary;
import com.soomgil.trip.infrastructure.persistence.mapper.TripQueryMapper;
import com.soomgil.trip.infrastructure.persistence.row.TripRow;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 검색용 여행방 검색 서비스.
 *
 * <p>요청자가 ACTIVE 멤버인 여행방 중 제목/목적지에 키워드가 매칭되는 항목을 반환한다.
 * 공개 여행방 검색은 보안상 지원하지 않는다.
 */
@Service
public class TripSearchService {

	private final TripQueryMapper tripQueryMapper;

	public TripSearchService(TripQueryMapper tripQueryMapper) {
		this.tripQueryMapper = tripQueryMapper;
	}

	@Transactional(readOnly = true)
	public List<TripSummary> searchMyTrips(UUID userId, String q, int size) {
		return tripQueryMapper.searchMyTrips(userId, q, size, 0).stream()
			.map(row -> toSummary(row, userId))
			.toList();
	}

	private TripSummary toSummary(TripRow row, UUID userId) {
		return new TripSummary(
			row.id(),
			row.title(),
			row.displayDestination(),
			TripStatus.valueOf(row.status()),
			row.ownerUserId().equals(userId) ? TripAccessRole.OWNER : TripAccessRole.MEMBER,
			row.itineraryVersion(),
			row.startDate(),
			row.endDate(),
			row.coverImageUrl(),
			row.createdAt() == null ? java.time.OffsetDateTime.now() : row.createdAt().atOffset(java.time.ZoneOffset.UTC)
		);
	}
}
