package com.soomgil.user.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;

/**
 * 즉시 계정 탈퇴 명령.
 *
 * <p>공유 데이터의 참조를 보존하기 위해 계정 root는 유지하지만, 상태를 {@code DELETED}로 바꾸고
 * 인증 수단과 개인정보를 같은 transaction에서 폐기·익명화한다.
 * 요청자가 {@code owner_user_id}인 활성 여행방이 있으면 거절한다(MVP OWNER 이관 불가 정책).
 *
 * @param userId 현재 로그인 사용자 식별자
 */
public record RequestAccountDeletionCommand(
	java.util.UUID userId
) implements Command<NoResult> {
}
