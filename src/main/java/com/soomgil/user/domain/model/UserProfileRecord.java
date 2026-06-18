package com.soomgil.user.domain.model;

import com.soomgil.user.api.dto.UserProfileVisibility;
import java.net.URI;
import java.util.UUID;

/**
 * {@code auth.user_profiles} row의 읽기/쓰기에 사용하는 user 도메인 persistence record.
 *
 * <p>{@link com.soomgil.user.api.dto.UserProfile} API DTO와 구분한다.
 * persistence의 모든 컬럼을 그대로 담아 application 계층에서 필요한 만큼 사용한다.
 *
 * @param userId 사용자 식별자
 * @param displayName 표시 이름
 * @param profileImageUrl 프로필 이미지 URL. 없으면 {@code null}
 * @param profileMediaFileId 프로필 미디어 파일 식별자. 없으면 {@code null}
 * @param bio 자기소개. 없으면 {@code null}
 * @param profileVisibility 프로필 공개 범위
 */
public record UserProfileRecord(
	UUID userId,
	String displayName,
	URI profileImageUrl,
	UUID profileMediaFileId,
	String bio,
	UserProfileVisibility profileVisibility
) {
}
