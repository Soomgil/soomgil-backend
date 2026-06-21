package com.soomgil.planning.application.service;

import com.soomgil.planning.application.event.PlanningRealtimeEvent;

/**
 * {@link PlanningEventBroadcaster}의 stub 구현.
 *
 * <p>WebSocket/STOMP 인프라가 구축되기 전까지 사용된다. 이벤트를 무시한다.
 *
 * <p>{@link com.soomgil.planning.application.config.PlanningModuleConfig}에서
 * {@code @ConditionalOnMissingBean}으로 등록되므로, 인프라 팀이
 * {@link PlanningEventBroadcaster} 구현체 bean을 등록하면 이 stub은 자동으로 비활성화된다.
 */
public class NoOpPlanningEventBroadcaster implements PlanningEventBroadcaster {

	@Override
	public void broadcast(PlanningRealtimeEvent event) {
		// TODO: global/realtime 인프라 팀이 실제 WebSocket broadcaster로 교체
		// 인프라 연동 전까지 mutation은 정상 동작하되 실시간 push는 생략된다.
	}
}
