package com.soomgil.user.domain.model;

import java.util.UUID;

/**
 * 사용자 검색 결과의 단일 row.
 *
 * <p>{@code auth.user_profiles}에서 {@code PUBLIC} 가시성만 읽어왔으므로,
 * 본 record는 {@code profile_visibility} 필드를 가지지 않는다.
 *
 * @param userId 사용자 식별자
 * @param displayName 표시 이름
 * @param profileImageUrl 프로필 이미지 URL. 없으면 {@code null}
 */
public record UserSummaryRecord(
	UUID userId,
	String displayName,
	String profileImageUrl
) {
}
