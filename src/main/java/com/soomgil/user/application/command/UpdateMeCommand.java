package com.soomgil.user.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.user.api.dto.User;
import java.util.UUID;

/**
 * 현재 로그인 사용자의 프로필을 부분 update하는 명령.
 *
 * <p>{@code /me} PATCH API 요청을 처리한다. 각 필드가 {@code null}이면 "변경 없음"을 의미한다.
 * 회원가입 시 기본 profile row가 생성되므로, 흐름 내에서 row를 신규 생성하지 않고
 * 기존 row를 갱신한다.
 *
 * <p>{@code profileMediaFileId}는 media 모듈(민경철) 연동 전까지는 단순히 UUID로 저장하고
 * 유효성 검증을 생략한다. media 모듈 완성 후 cross-domain 검증을 추가한다.
 *
 * @param userId 현재 로그인 사용자 식별자
 * @param displayName 표시 이름. 1~80자. {@code null}이면 변경 없음
 * @param profileMediaFileId 프로필 미디어 파일 식별자. {@code null}이면 변경 없음
 * @param bio 자기소개. 최대 500자. {@code null}이면 변경 없음
 * @param profileVisibility 프로필 공개 범위. {@code null}이면 변경 없음
 */
public record UpdateMeCommand(
	UUID userId,
	String displayName,
	UUID profileMediaFileId,
	String bio,
	com.soomgil.user.api.dto.UserProfileVisibility profileVisibility
) implements Command<User> {
}
