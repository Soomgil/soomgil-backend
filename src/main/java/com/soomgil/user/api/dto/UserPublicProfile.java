package com.soomgil.user.api.dto;

import com.soomgil.preference.api.dto.MyPreferenceSummary;
import com.soomgil.preference.api.dto.SavedPlace;
import com.soomgil.social.api.dto.FollowStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * 다른 사용자에게 노출할 프로필과 공개 가능한 여행 취향 정보를 묶은 응답.
 *
 * <p>{@code PUBLIC} 프로필과 본인 또는 승인된 follower가 조회하는 {@code PRIVATE} 프로필에는
 * {@code superLikedPlaces}와 {@code preferences}가 포함된다. 권한이 없는 {@code PRIVATE} 프로필은
 * 장소를 빈 목록으로 반환하고 취향 정보는 {@code null}로 숨긴다.
 */
public record UserPublicProfile(
	@NotNull
	UUID id,
	@NotBlank
	String displayName,
	URI profileImageUrl,
	String bio,
	@Min(0)
	Integer followerCount,
	@Min(0)
	Integer followingCount,
	Boolean followedByMe,
	FollowStatus followStatus,
	UserProfileVisibility profileVisibility,
	@Valid
	List<SavedPlace> superLikedPlaces,
	@Valid
	MyPreferenceSummary preferences
) {
}
