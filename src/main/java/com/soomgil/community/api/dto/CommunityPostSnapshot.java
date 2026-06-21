package com.soomgil.community.api.dto;

import com.soomgil.itinerary.api.dto.ItineraryDay;
import com.soomgil.itinerary.api.dto.RouteSegment;
import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 커뮤니티 게시글 발행 시점의 여행 일정 snapshot.
 *
 * <p>게시글이 발행된 이후 원본 여행방이 변경되어도 이 snapshot은 변하지 않는다.
 * 발행자가 노출을 제한한 경우 {@code authorDisplay}는 익명 처리될 수 있다.
 *
 * @param days 일자별 일정 목록
 * @param routes 구간별 경로 세그먼트 목록
 * @param authorDisplay snapshot에 표시되는 발행자 정보
 */
public record CommunityPostSnapshot(
	@Valid
	List<ItineraryDay> days,
	@Valid
	List<RouteSegment> routes,
	@Valid
	UserSummary authorDisplay
) {
}
