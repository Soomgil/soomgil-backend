package com.soomgil.voting.application.port;

import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.util.UUID;

/** 여행방 투표 상태 변경을 실시간 구독자에게 알린다. */
public interface VoteRealtimePublisher {

	/**
	 * 투표 세션 상태 변경 이벤트를 발행한다.
	 *
	 * @param tripId 여행방 ID
	 * @param sessionId 투표 세션 ID
	 * @param status 변경 후 세션 상태
	 */
	void publish(UUID tripId, UUID sessionId, VoteSessionStatus status);
}
