package com.soomgil.planning.application.service;

import com.soomgil.planning.application.event.PlanningRealtimeEvent;

/**
 * planning 도메인 이벤트를 같은 여행방 참여자에게 브로드캐스트한다.
 *
 * <p>구현체는 WebSocket/STOMP topic으로 변환해 메시지를 전송한다. 분산 환경에서는 Redis
 * Pub/Sub 등을 통해 다른 인스턴스에 연결된 클라이언트에게도 전달한다.
 *
 * <p>현재 구현체는 transaction commit 이후 여행방 planning STOMP topic으로 전송한다.
 */
public interface PlanningEventBroadcaster {

	/**
	 * 이벤트를 같은 여행방 참여자에게 브로드캐스트한다.
	 *
	 * <p>브로드캐스트 실패는 호출부의 트랜잭션에 영향을 주지 않는다 (best-effort).
	 * 실시간 동기화는 mutation 자체의 정합성과 독립적인 부가 기능이기 때문.
	 *
	 * @param event 발행할 이벤트
	 */
	void broadcast(PlanningRealtimeEvent event);
}
