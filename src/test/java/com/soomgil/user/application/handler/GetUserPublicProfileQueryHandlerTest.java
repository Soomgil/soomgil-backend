package com.soomgil.user.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.preference.api.dto.MyPreferenceSummary;
import com.soomgil.preference.api.dto.PagedSavedPlace;
import com.soomgil.preference.application.command.handler.PreferenceSavedPlaceService;
import com.soomgil.preference.application.query.handler.PreferenceUserPreferenceQueryService;
import com.soomgil.user.api.dto.UserProfileVisibility;
import com.soomgil.user.api.dto.UserPublicProfile;
import com.soomgil.user.application.query.GetUserPublicProfileQuery;
import com.soomgil.user.domain.model.UserProfileRecord;
import com.soomgil.user.infrastructure.persistence.UserPublicProfileMapper;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.soomgil.social.infrastructure.persistence.UserFollowRecord;

/**
 * {@link GetUserPublicProfileQueryHandler} 단위 테스트.
 */
class GetUserPublicProfileQueryHandlerTest {

	private final UserPublicProfileMapper mapper = mock(UserPublicProfileMapper.class);
	private final com.soomgil.social.infrastructure.persistence.UserFollowMapper userFollowMapper =
		mock(com.soomgil.social.infrastructure.persistence.UserFollowMapper.class);
	private final PreferenceSavedPlaceService savedPlaceService = mock(PreferenceSavedPlaceService.class);
	private final PreferenceUserPreferenceQueryService preferenceQueryService =
		mock(PreferenceUserPreferenceQueryService.class);
	private final GetUserPublicProfileQueryHandler handler =
		new GetUserPublicProfileQueryHandler(
			mapper, userFollowMapper, savedPlaceService, preferenceQueryService
		);

	@Test
	@DisplayName("PUBLIC 프로필은 자기소개를 포함한 전체 정보를 반환한다")
	void returnsFullProfileForPublic() {
		UUID viewerId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		String imageUrl = "https://cdn.example.com/minji.png";
		UserProfileRecord record = new UserProfileRecord(
			targetId, "민지", imageUrl, null, "안녕하세요", UserProfileVisibility.PUBLIC
		);
		when(mapper.findByUserId(targetId)).thenReturn(Optional.of(record));
		when(userFollowMapper.countFollowers(targetId)).thenReturn(5);
		when(userFollowMapper.countFollowing(targetId)).thenReturn(10);
		when(userFollowMapper.find(viewerId, targetId)).thenReturn(Optional.empty());
		stubVisibleDetails(targetId);

		UserPublicProfile result = handler.handle(new GetUserPublicProfileQuery(viewerId, targetId));

		assertThat(result.id()).isEqualTo(targetId);
		assertThat(result.displayName()).isEqualTo("민지");
		assertThat(result.profileImageUrl()).isEqualTo(URI.create(imageUrl));
		assertThat(result.bio()).isEqualTo("안녕하세요");
		assertThat(result.profileVisibility()).isEqualTo(UserProfileVisibility.PUBLIC);
		assertThat(result.followerCount()).isEqualTo(5);
		assertThat(result.followingCount()).isEqualTo(10);
		assertThat(result.followedByMe()).isFalse();
		assertThat(result.followStatus()).isNull();
		assertThat(result.superLikedPlaces()).isEmpty();
		assertThat(result.preferences()).isNotNull();
	}

	@Test
	@DisplayName("PRIVATE 프로필은 자기소개를 숨기고 제한 요약만 반환한다")
	void returnsLimitedProfileForPrivate() {
		UUID viewerId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		UserProfileRecord record = new UserProfileRecord(
			targetId, "현우", null, null, "비공개 자기소개", UserProfileVisibility.PRIVATE
		);
		when(mapper.findByUserId(targetId)).thenReturn(Optional.of(record));
		when(userFollowMapper.countFollowers(targetId)).thenReturn(0);
		when(userFollowMapper.countFollowing(targetId)).thenReturn(0);
		when(userFollowMapper.find(viewerId, targetId)).thenReturn(Optional.empty());

		UserPublicProfile result = handler.handle(new GetUserPublicProfileQuery(viewerId, targetId));

		assertThat(result.id()).isEqualTo(targetId);
		assertThat(result.displayName()).isEqualTo("현우");
		assertThat(result.bio()).isNull();
		assertThat(result.profileVisibility()).isEqualTo(UserProfileVisibility.PRIVATE);
		assertThat(result.superLikedPlaces()).isEmpty();
		assertThat(result.preferences()).isNull();
		verify(savedPlaceService, never()).listForUser(targetId, 0, 100);
		verify(preferenceQueryService, never()).listPreferences(targetId);
	}

	@Test
	@DisplayName("승인된 팔로워는 PRIVATE 프로필의 자기소개를 볼 수 있다")
	void returnsPrivateProfileDetailForApprovedFollower() {
		UUID viewerId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		when(mapper.findByUserId(targetId)).thenReturn(Optional.of(new UserProfileRecord(
			targetId, "현우", null, null, "비공개 자기소개", UserProfileVisibility.PRIVATE
		)));
		when(userFollowMapper.find(viewerId, targetId)).thenReturn(Optional.of(new UserFollowRecord(
			viewerId, targetId, "ACTIVE", java.time.Instant.now(), java.time.Instant.now()
		)));
		stubVisibleDetails(targetId);

		UserPublicProfile result = handler.handle(new GetUserPublicProfileQuery(viewerId, targetId));

		assertThat(result.bio()).isEqualTo("비공개 자기소개");
		assertThat(result.preferences()).isNotNull();
	}

	@Test
	@DisplayName("대상 사용자의 profile row가 없으면 USER_NOT_FOUND 예외를 던진다")
	void throwsUserNotFoundWhenProfileMissing() {
		UUID viewerId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		when(mapper.findByUserId(targetId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new GetUserPublicProfileQuery(viewerId, targetId)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	private void stubVisibleDetails(UUID targetId) {
		when(savedPlaceService.listForUser(targetId, 0, 100)).thenReturn(new PagedSavedPlace(
			List.of(), new PageMeta(0, 100, 0L, 0, List.of("createdAt,desc"))
		));
		when(preferenceQueryService.listPreferences(targetId)).thenReturn(
			new MyPreferenceSummary(List.of(), "아직 학습된 취향이 없어요.", List.of())
		);
	}
}
