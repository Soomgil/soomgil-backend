package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.List;
import java.util.UUID;

/**
 * 여행방 전체 active 일정 snapshot 재정렬 command.
 *
 * <p>부분 delta가 아니라 day와 item의 전체 순서 snapshot을 받아 같은 transaction에서 적용한다.
 */
public record ReorderItineraryCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	List<ItineraryDayOrderCommand> days
) implements Command<ItineraryMutationResult> {

	public ReorderItineraryCommand {
		days = days == null ? List.of() : List.copyOf(days);
	}
}
