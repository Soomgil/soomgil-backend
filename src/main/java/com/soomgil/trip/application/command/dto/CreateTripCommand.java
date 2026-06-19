package com.soomgil.trip.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.List;
import java.util.UUID;

/**
 * 새 여행방 생성을 요청하는 command.
 *
 * <p>호출자는 이미 인증된 사용자 ID를 {@code creatorUserId}에 담아야 한다.
 * handler는 여행방과 생성자의 최초 active member row를 같은 transaction에서 만든다.
 */
public record CreateTripCommand(
	UUID creatorUserId,
	String title,
	String displayDestination,
	List<String> legalRegionCodes
) implements Command<CreateTripResult> {

	public CreateTripCommand {
		legalRegionCodes = legalRegionCodes == null ? List.of() : List.copyOf(legalRegionCodes);
	}
}
