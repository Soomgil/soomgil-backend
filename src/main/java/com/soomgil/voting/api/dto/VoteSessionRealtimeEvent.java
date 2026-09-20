package com.soomgil.voting.api.dto;

import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.util.UUID;

/** 여행방 투표 상태 변경을 알리는 STOMP 메시지. */
public record VoteSessionRealtimeEvent(
	String eventType,
	UUID tripId,
	UUID sessionId,
	VoteSessionStatus status
) {
}
