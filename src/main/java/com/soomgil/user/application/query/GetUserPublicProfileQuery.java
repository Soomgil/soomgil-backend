package com.soomgil.user.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.user.api.dto.UserPublicProfile;
import java.util.UUID;

/**
 * 사용자 공개 프로필 조회 요청.
 *
 * <p>대상 사용자의 {@code profile_visibility}에 따라 응답 범위가 다르다.
 * {@code PUBLIC}이면 전체 프로필(자기소개 포함)을 반환하고,
 * {@code PRIVATE}이면 제한된 요약(id, displayName, profileImageUrl, visibility)만 반환한다.
 *
 * <p>{@code viewerUserId}는 social 모듈(민경철)의 follow status 연동에 사용된다.
 * 모듈 연동 전까지는 {@code viewerUserId}가 승인된 follower인지 검증하지 않고
 * {@code PRIVATE} 프로필은 항상 제한 요약만 반환한다.
 *
 * @param viewerUserId 조회하는 사용자 식별자. 인증된 호출자
 * @param targetUserId 조회 대상 사용자 식별자
 */
public record GetUserPublicProfileQuery(
	UUID viewerUserId,
	UUID targetUserId
) implements Query<UserPublicProfile> {
}
