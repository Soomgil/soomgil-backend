package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;

import java.util.List;
import java.util.UUID;

/**
 * 이메일/비밀번호 회원가입 요청.
 *
 * @param email 사용자 이메일
 * @param password raw 비밀번호
 * @param displayName 표시 이름
 * @param acceptedPolicyDocumentIds 동의한 필수 약관 ID 목록
 */
public record RegisterCommand(
	String email,
	String password,
	String displayName,
	List<UUID> acceptedPolicyDocumentIds
) implements Command<RegisterResult> {
}
