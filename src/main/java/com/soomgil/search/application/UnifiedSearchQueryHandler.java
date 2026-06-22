package com.soomgil.search.application;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.community.application.CommunitySearchService;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.search.api.dto.UnifiedSearchResponse;
import com.soomgil.search.api.dto.UserSearchResult;
import com.soomgil.trip.application.TripSearchService;
import com.soomgil.user.application.UserSearchService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 검색 쿼리 핸들러.
 *
 * <p>4개 도메인(trip/place/community/user)을 병렬로 검색해 한 응답에 취합한다.
 * 각 섹션은 {@link UnifiedSearchQuery#size()} 개수만큼(기본 4)만 노출한다.
 * - trips: 본인 소속 여행방, created_at DESC
 * - places: 키워드 매칭 장소
 * - posts: 공개 게시글, 좋아요 수 DESC
 * - users: 팔로워 수 DESC
 */
@Component
public class UnifiedSearchQueryHandler implements QueryHandler<UnifiedSearchQuery, UnifiedSearchResponse> {

	private static final int DEFAULT_SECTION_SIZE = 4;
	private static final int MAX_SECTION_SIZE = 10;

	private final TripSearchService tripSearchService;
	private final PlaceSearchQueryHandler placeSearchHandler;
	private final CommunitySearchService communitySearchService;
	private final UserSearchService userSearchService;

	public UnifiedSearchQueryHandler(
		TripSearchService tripSearchService,
		PlaceSearchQueryHandler placeSearchHandler,
		CommunitySearchService communitySearchService,
		UserSearchService userSearchService
	) {
		this.tripSearchService = tripSearchService;
		this.placeSearchHandler = placeSearchHandler;
		this.communitySearchService = communitySearchService;
		this.userSearchService = userSearchService;
	}

	@Override
	@Transactional(readOnly = true)
	public UnifiedSearchResponse handle(UnifiedSearchQuery query) {
		String q = normalizeQuery(query.query());
		int size = clampSize(query.size());
		UUID requesterUserId = query.requesterUserId();

		var trips = tripSearchService.searchMyTrips(requesterUserId, q, size);
		var places = placeSearchHandler.handle(new PlaceSearchQuery(q, null, null, null, 0, size)).items();
		var posts = communitySearchService.searchPublicPostsByLikes(q, size, requesterUserId);
		List<UserSearchResult> users = userSearchService.searchWithFollowerCount(q, size);

		return new UnifiedSearchResponse(q, trips, places, posts, users);
	}

	private String normalizeQuery(String raw) {
		if (raw == null) return "";
		String trimmed = raw.trim();
		return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
	}

	private int clampSize(int size) {
		if (size <= 0) return DEFAULT_SECTION_SIZE;
		return Math.min(size, MAX_SECTION_SIZE);
	}
}
