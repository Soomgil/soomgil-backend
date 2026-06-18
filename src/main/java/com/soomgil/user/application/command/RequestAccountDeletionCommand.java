package com.soomgil.user.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;

/**
 * 계정 삭제 예약 명령.
 *
 * <p>즉시 hard delete하지 않고, {@code auth.users.status}를 {@code PENDING_DELETION}으로
 * 변경하고 {@code deletion_requested_at}, {@code deletion_scheduled_at}을 기록한다.
 * 요청자가 {@code owner_user_id}인 활성 여행방이 있으면 거절한다(MVP OWNER 이관 불가 정책).
 *
 * @param userId 현재 로그인 사용자 식별자
 */
public record RequestAccountDeletionCommand(
	java.util.UUID userId
) implements Command<NoResult> {
}
