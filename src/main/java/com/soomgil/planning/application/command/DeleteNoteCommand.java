package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.UUID;

/**
 * note soft delete 요청.
 *
 * <p>요청한 여행방에 속한 note만 찾아 {@code deleted_at}을 설정한다. 이미 삭제됐거나
 * 다른 여행방의 note이면 {@link com.soomgil.global.error.ErrorCode#PLANNING_NOTE_NOT_FOUND},
 * 버전이 달라졌으면 {@link com.soomgil.global.error.ErrorCode#PLANNING_VERSION_CONFLICT}이다.
 *
 * @param tripId 여행방 식별자
 * @param noteId note 식별자
 * @param actorUserId 요청자
 * @param baseVersion 클라이언트가 마지막으로 읽은 메모 버전
 */
public record DeleteNoteCommand(
	UUID tripId,
	UUID noteId,
	UUID actorUserId,
	long baseVersion
) implements Command<PlanningMutationResponse> {
}
