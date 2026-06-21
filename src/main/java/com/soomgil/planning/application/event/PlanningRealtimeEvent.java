package com.soomgil.planning.application.event;

import java.util.UUID;

/**
 * planning 도메인에서 발생하는 실시간 동기화 이벤트의 공통 계약.
 *
 * <p>모든 mutation handler는 변경 완료 후 구체 이벤트 record를 {@link
 * com.soomgil.planning.application.service.PlanningEventBroadcaster}에 전달한다.
 * broadcaster 구현체가 WebSocket/STOMP topic으로 변환해 같은 여행방 참여자에게 브로드캐스트한다.
 *
 * <p>이벤트는 다음 정보를 반드시 포함한다:
 * <ul>
 *   <li>{@code tripId} - 어느 여행방에 브로드캐스트할지 결정</li>
 *   <li>{@code actorUserId} - 누가 변경했는지 (중복 echo 방지용)</li>
 *   <li>{@code eventType} - 클라이언트가 구독 필터링/라우팅에 사용하는 문자열 키</li>
 * </ul>
 */
public interface PlanningRealtimeEvent {

	/**
	 * 이벤트가 발생한 여행방 식별자.
	 *
	 * @return 여행방 식별자
	 */
	UUID tripId();

	/**
	 * 변경을 수행한 사용자 식별자.
	 *
	 * <p>클라이언트는 이 값이 자신의 userId와 일치하면 자신이 보낸 변경 echo로 간주하고
	 * 로컬 상태 갱신을 건너뛸 수 있다 (optimistic local update 패턴).
	 *
	 * @return 요청자 식별자
	 */
	UUID actorUserId();

	/**
	 * 이벤트 타입 문자열. 클라이언트 라우팅/필터링 키로 사용.
	 *
	 * <p>규칙: {@code "planning.<resource>.<verb>"} (예: {@code "planning.note.upserted"})
	 *
	 * @return 이벤트 타입
	 */
	String eventType();
}
