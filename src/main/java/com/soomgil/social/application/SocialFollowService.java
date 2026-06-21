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
		if (page < 0 || size < 1 || size > 100) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "page or size is out of range.");
		}
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

	private OffsetDateTime offset(Instant value) {
		return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
	}

	private URI uri(String value) {
		return value == null || value.isBlank() ? null : URI.create(value);
	}
}
