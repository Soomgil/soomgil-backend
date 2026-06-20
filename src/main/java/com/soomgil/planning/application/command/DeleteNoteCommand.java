package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.UUID;

/**
 * note soft delete 요청.
 *
 * <p>식별자로 note를 찾고 {@code deleted_at}을 설정한다. 이미 삭제됐으면
 * {@link com.soomgil.global.error.ErrorCode#PLANNING_NOTE_NOT_FOUND}.
 *
 * @param tripId 여행방 식별자
 * @param noteId note 식별자
 * @param actorUserId 요청자
 */
public record DeleteNoteCommand(
	UUID tripId,
	UUID noteId,
	UUID actorUserId
) implements Command<PlanningMutationResponse> {
}
