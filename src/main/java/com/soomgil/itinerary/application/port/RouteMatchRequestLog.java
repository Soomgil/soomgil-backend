package com.soomgil.itinerary.application.port;

import java.time.Instant;
import java.util.UUID;

/**
 * route map matching 요청 이력 저장 모델.
 *
 * @param tripId 여행방 ID
 * @param tripRouteId 생성된 route ID
 * @param originItineraryItemId 출발 일정 item ID
 * @param destinationItineraryItemId 도착 일정 item ID
 * @param requestedByUserId 요청 사용자 ID
 * @param provider provider 이름
 * @param providerProfile provider profile
 * @param inputCoordinates 입력 좌표 JSON
 * @param radiuses 탐색 반경 JSON
 * @param tidy tidy 옵션
 * @param requestHash 요청 hash
 * @param status 처리 상태
 * @param confidence matching 신뢰도
 * @param distanceMeters 거리(m)
 * @param durationSeconds 소요 시간(초)
 * @param tracepoints tracepoints JSON
 * @param matchingsMetadata matchings metadata JSON
 * @param errorCode 실패 code
 * @param errorMessage 실패 메시지
 * @param createdAt 생성 시각
 * @param completedAt 완료 시각
 */
public record RouteMatchRequestLog(
	UUID tripId,
	UUID tripRouteId,
	UUID originItineraryItemId,
	UUID destinationItineraryItemId,
	UUID requestedByUserId,
	String provider,
	String providerProfile,
	String inputCoordinates,
	String radiuses,
	Boolean tidy,
	String requestHash,
	String status,
	Double confidence,
	Double distanceMeters,
	Double durationSeconds,
	String tracepoints,
	String matchingsMetadata,
	String errorCode,
	String errorMessage,
	Instant createdAt,
	Instant completedAt
) {
}
