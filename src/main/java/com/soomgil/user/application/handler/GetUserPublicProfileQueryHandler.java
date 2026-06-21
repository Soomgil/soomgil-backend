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
 * {@code followStatus})는 social 모듈(민경철) 데이터가 필요하다. social 모듈의
 * {@link com.soomgil.social.infrastructure.persistence.UserFollowMapper}를 통해 실제 데이터를 연동하여 제공한다.
 */
@Component
@Transactional(readOnly = true)
public class GetUserPublicProfileQueryHandler
	implements QueryHandler<GetUserPublicProfileQuery, UserPublicProfile> {

	private final UserPublicProfileMapper userPublicProfileMapper;
	private final com.soomgil.social.infrastructure.persistence.UserFollowMapper userFollowMapper;

	public GetUserPublicProfileQueryHandler(
		UserPublicProfileMapper userPublicProfileMapper,
		com.soomgil.social.infrastructure.persistence.UserFollowMapper userFollowMapper
	) {
		this.userPublicProfileMapper = userPublicProfileMapper;
		this.userFollowMapper = userFollowMapper;
	}

	@Override
	public UserPublicProfile handle(GetUserPublicProfileQuery query) {
		UserProfileRecord record = userPublicProfileMapper.findByUserId(query.targetUserId())
			.orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND,
				"User not found: " + query.targetUserId()));

		boolean isPrivate = record.profileVisibility() == UserProfileVisibility.PRIVATE;
		String bio = isPrivate ? null : record.bio();

		// social 모듈 데이터 조회
		int followerCount = userFollowMapper.countFollowers(query.targetUserId());
		int followingCount = userFollowMapper.countFollowing(query.targetUserId());

		// 로그인한 사용자가 대상 사용자를 팔로우하는지 여부 및 상태 조회
		com.soomgil.social.infrastructure.persistence.UserFollowRecord followRecord = userFollowMapper.find(
			query.viewerUserId(), query.targetUserId()
		).orElse(null);

		Boolean followedByMe = false;
		com.soomgil.social.api.dto.FollowStatus followStatus = null;

		if (followRecord != null) {
			String statusStr = followRecord.status();
			if ("ACTIVE".equals(statusStr) || "PENDING".equals(statusStr) || "DELETED".equals(statusStr)) {
				followStatus = com.soomgil.social.api.dto.FollowStatus.valueOf(statusStr);
			}
			followedByMe = "ACTIVE".equals(statusStr);
		}

		return new UserPublicProfile(
			record.userId(),
			record.displayName(),
			record.profileImageUrl() != null ? java.net.URI.create(record.profileImageUrl()) : null,
			bio,
			followerCount,
			followingCount,
			followedByMe,
			followStatus,
			record.profileVisibility()
		);
	}
}
