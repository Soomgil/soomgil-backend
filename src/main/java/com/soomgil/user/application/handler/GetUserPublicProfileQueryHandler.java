package com.soomgil.user.application.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.api.dto.UserPublicProfile;
import com.soomgil.user.api.dto.UserProfileVisibility;
import com.soomgil.user.application.query.GetUserPublicProfileQuery;
import com.soomgil.user.domain.model.UserException;
import com.soomgil.user.domain.model.UserProfileRecord;
import com.soomgil.user.infrastructure.persistence.UserPublicProfileMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 공개 프로필 조회를 처리한다.
 *
 * <p>가시성 규칙:
 * <ul>
 *   <li>{@code PUBLIC}: 전체 프로필(display name, profile image URL, bio, visibility) 반환.</li>
 *   <li>{@code PRIVATE}: 제한 요약(id, displayName, profileImageUrl, visibility)만 반환.
 *       자기소개({@code bio})는 노출하지 않는다.</li>
 * </ul>
 *
 * <p>follow 관련 필드({@code followerCount}, {@code followingCount}, {@code followedByMe},
 * {@code followStatus})는 social 모듈(민경철) 데이터가 필요하다. 모듈 연동 전까지는
 * {@code null}로 반환한다.
 */
@Component
@Transactional(readOnly = true)
public class GetUserPublicProfileQueryHandler
	implements QueryHandler<GetUserPublicProfileQuery, UserPublicProfile> {

	private final UserPublicProfileMapper userPublicProfileMapper;

	public GetUserPublicProfileQueryHandler(UserPublicProfileMapper userPublicProfileMapper) {
		this.userPublicProfileMapper = userPublicProfileMapper;
	}

	@Override
	public UserPublicProfile handle(GetUserPublicProfileQuery query) {
		UserProfileRecord record = userPublicProfileMapper.findByUserId(query.targetUserId())
			.orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND,
				"User not found: " + query.targetUserId()));

		boolean isPrivate = record.profileVisibility() == UserProfileVisibility.PRIVATE;
		String bio = isPrivate ? null : record.bio();

		return new UserPublicProfile(
			record.userId(),
			record.displayName(),
			record.profileImageUrl(),
			bio,
			null,
			null,
			null,
			null,
			record.profileVisibility()
		);
	}
}
