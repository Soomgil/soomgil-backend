package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;
import java.util.List;
import java.util.UUID;

/**
 * 소셜 로그인 신규 가입자 온보딩(닉네임 설정 및 필수 약관 동의) 요청 command.
 *
 * @param userId 사용자 식별자
 * @param displayName 설정할 표시 이름 (닉네임)
 * @param acceptedPolicyDocumentIds 동의한 필수 약관 ID 목록
 */
public record OnboardCommand(
	UUID userId,
	String displayName,
	List<UUID> acceptedPolicyDocumentIds
) implements Command<AuthTokenResult> {
}
