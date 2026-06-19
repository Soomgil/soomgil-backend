package com.soomgil.user.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.api.dto.UserProfileVisibility;
import com.soomgil.user.api.dto.UserPublicProfile;
import com.soomgil.user.application.query.GetUserPublicProfileQuery;
import com.soomgil.user.domain.model.UserProfileRecord;
import com.soomgil.user.infrastructure.persistence.UserPublicProfileMapper;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link GetUserPublicProfileQueryHandler} 단위 테스트.
 */
class GetUserPublicProfileQueryHandlerTest {

	private final UserPublicProfileMapper mapper = mock(UserPublicProfileMapper.class);
	private final GetUserPublicProfileQueryHandler handler =
		new GetUserPublicProfileQueryHandler(mapper);

	@Test
	@DisplayName("PUBLIC 프로필은 자기소개를 포함한 전체 정보를 반환한다")
	void returnsFullProfileForPublic() {
		UUID viewerId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		URI imageUrl = URI.create("https://cdn.example.com/minji.png");
		UserProfileRecord record = new UserProfileRecord(
			targetId, "민지", imageUrl, null, "안녕하세요", UserProfileVisibility.PUBLIC
		);
		when(mapper.findByUserId(targetId)).thenReturn(Optional.of(record));

		UserPublicProfile result = handler.handle(new GetUserPublicProfileQuery(viewerId, targetId));

		assertThat(result.id()).isEqualTo(targetId);
		assertThat(result.displayName()).isEqualTo("민지");
		assertThat(result.profileImageUrl()).isEqualTo(imageUrl);
		assertThat(result.bio()).isEqualTo("안녕하세요");
		assertThat(result.profileVisibility()).isEqualTo(UserProfileVisibility.PUBLIC);
		assertThat(result.followerCount()).isNull();
		assertThat(result.followingCount()).isNull();
		assertThat(result.followedByMe()).isNull();
		assertThat(result.followStatus()).isNull();
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

		UserPublicProfile result = handler.handle(new GetUserPublicProfileQuery(viewerId, targetId));

		assertThat(result.id()).isEqualTo(targetId);
		assertThat(result.displayName()).isEqualTo("현우");
		assertThat(result.bio()).isNull();
		assertThat(result.profileVisibility()).isEqualTo(UserProfileVisibility.PRIVATE);
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
}
