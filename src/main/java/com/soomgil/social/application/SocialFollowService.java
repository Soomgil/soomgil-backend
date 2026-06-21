package com.soomgil.social.application;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.social.api.dto.Follow;
import com.soomgil.social.api.dto.FollowRequest;
import com.soomgil.social.api.dto.FollowStatus;
import com.soomgil.social.api.dto.PagedFollowRequest;
import com.soomgil.social.application.port.SocialFollowRepository;
import com.soomgil.social.domain.model.SocialFollowRecord;
import com.soomgil.user.api.dto.PagedUserSummary;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공개/비공개 프로필 정책에 따라 follow 관계와 승인 요청을 관리한다. */
@Service
public class SocialFollowService {

	private final SocialFollowRepository repository;
	private final TimeProvider timeProvider;

	public SocialFollowService(SocialFollowRepository repository, TimeProvider timeProvider) {
		this.repository = repository;
		this.timeProvider = timeProvider;
	}

	@Transactional
	public Follow follow(UUID currentUserId, UUID targetUserId) {
		if (currentUserId.equals(targetUserId)) {
			throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
		}
		String visibility = repository.findProfileVisibility(targetUserId);
		if (visibility == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User was not found.");
		}
		String status = "PRIVATE".equals(visibility) ? "PENDING" : "ACTIVE";
		return toFollow(repository.upsert(currentUserId, targetUserId, status, timeProvider.now()));
	}

	@Transactional
	public void unfollow(UUID currentUserId, UUID targetUserId) {
		if (!repository.delete(currentUserId, targetUserId, null, timeProvider.now())) {
			throw new BusinessException(ErrorCode.FOLLOW_NOT_FOUND);
		}
	}

	@Transactional(readOnly = true)
	public PagedFollowRequest listPending(UUID currentUserId, int page, int size) {
		validatePage(page, size);
		long total = repository.countPending(currentUserId);
		List<FollowRequest> items = repository.findPending(currentUserId, page * size, size).stream()
			.map(row -> new FollowRequest(
				new UserSummary(row.followerUserId(), row.displayName(), uri(row.profileImageUrl())),
				"PENDING", offset(row.createdAt())
			))
			.toList();
		int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
		return new PagedFollowRequest(items, new PageMeta(page, size, total, totalPages, List.of()));
	}

	/**
	 * 사용자의 ACTIVE 팔로워를 최신 관계순으로 조회한다.
	 *
	 * <p>PUBLIC 프로필은 비로그인 사용자도 조회할 수 있다. PRIVATE 프로필은 본인만 조회할 수 있다.
	 */
	@Transactional(readOnly = true)
	public PagedUserSummary listFollowers(UUID viewerUserId, UUID targetUserId, int page, int size) {
		validateFollowListAccess(viewerUserId, targetUserId, page, size);
		long total = repository.countFollowers(targetUserId);
		List<UserSummary> items = repository.findFollowers(targetUserId, page * size, size).stream()
			.map(row -> new UserSummary(row.userId(), row.displayName(), uri(row.profileImageUrl())))
			.toList();
		return new PagedUserSummary(items, pageMeta(page, size, total));
	}

	/**
	 * 사용자가 ACTIVE 상태로 팔로우하는 사람을 최신 관계순으로 조회한다.
	 *
	 * <p>PUBLIC 프로필은 비로그인 사용자도 조회할 수 있다. PRIVATE 프로필은 본인만 조회할 수 있다.
	 */
	@Transactional(readOnly = true)
	public PagedUserSummary listFollowing(UUID viewerUserId, UUID targetUserId, int page, int size) {
		validateFollowListAccess(viewerUserId, targetUserId, page, size);
		long total = repository.countFollowing(targetUserId);
		List<UserSummary> items = repository.findFollowing(targetUserId, page * size, size).stream()
			.map(row -> new UserSummary(row.userId(), row.displayName(), uri(row.profileImageUrl())))
			.toList();
		return new PagedUserSummary(items, pageMeta(page, size, total));
	}

	@Transactional
	public Follow accept(UUID currentUserId, UUID followerUserId) {
		Instant now = timeProvider.now();
		if (!repository.activatePending(followerUserId, currentUserId, now)) {
			throw new BusinessException(ErrorCode.FOLLOW_REQUEST_NOT_FOUND);
		}
		return toFollow(repository.find(followerUserId, currentUserId));
	}

	@Transactional
	public void reject(UUID currentUserId, UUID followerUserId) {
		if (!repository.delete(followerUserId, currentUserId, "PENDING", timeProvider.now())) {
			throw new BusinessException(ErrorCode.FOLLOW_REQUEST_NOT_FOUND);
		}
	}

	private Follow toFollow(SocialFollowRecord value) {
		return new Follow(
			value.followerUserId(), value.followingUserId(), FollowStatus.valueOf(value.status()),
			offset(value.createdAt())
		);
	}

	private void validateFollowListAccess(UUID viewerUserId, UUID targetUserId, int page, int size) {
		validatePage(page, size);
		String visibility = repository.findProfileVisibility(targetUserId);
		if (visibility == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User was not found.");
		}
		if ("PRIVATE".equals(visibility) && !targetUserId.equals(viewerUserId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Private follow lists are visible only to the owner.");
		}
	}

	private void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "page or size is out of range.");
		}
	}

	private PageMeta pageMeta(int page, int size, long total) {
		int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
		return new PageMeta(page, size, total, totalPages, List.of());
	}

	private OffsetDateTime offset(Instant value) {
		return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
	}

	private URI uri(String value) {
		return value == null || value.isBlank() ? null : URI.create(value);
	}
}
