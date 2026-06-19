package com.soomgil.trip.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.trip.domain.model.TripStatus;
import java.util.List;
import java.util.UUID;

/**
 * 여행방 기본 설정 수정을 요청하는 command.
 *
 * <p>{@code legalRegionCodes}는 null이면 미변경, 빈 목록이면 전체 삭제로 해석한다.
 */
public record UpdateTripCommand(
	UUID tripId,
	UUID actorUserId,
	String title,
	String displayDestination,
	List<String> legalRegionCodes,
	TripStatus status
) implements Command<NoResult> {

	public UpdateTripCommand {
		legalRegionCodes = legalRegionCodes == null ? null : List.copyOf(legalRegionCodes);
	}

	public boolean legalRegionCodesProvided() {
		return legalRegionCodes != null;
	}
}
