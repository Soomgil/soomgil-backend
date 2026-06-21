package com.soomgil.social.api;

import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.social.api.dto.Follow;
import com.soomgil.social.api.dto.FollowRequest;
import com.soomgil.social.api.dto.FollowStatus;
import com.soomgil.social.api.dto.PagedFollowRequest;
import com.soomgil.social.infrastructure.persistence.UserFollowMapper;
import com.soomgil.social.infrastructure.persistence.UserFollowRecord;
import com.soomgil.social.infrastructure.persistence.UserSummaryRecord;
import com.soomgil.user.api.dto.UserProfileVisibility;
import com.soomgil.user.api.dto.UserSummary;
import com.soomgil.user.domain.model.UserProfileRecord;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SocialController extends ApiControllerSupport {

	private final UserFollowMapper userFollowMapper;
	private final UserProfileMapper userProfileMapper;

	public SocialController(
		UserFollowMapper userFollowMapper,
		UserProfileMapper userProfileMapper
	) {
		this.userFollowMapper = userFollowMapper;
		this.userProfileMapper = userProfileMapper;
	}

	@PostMapping("/users/{userId}/follow")
	@ResponseStatus(HttpStatus.CREATED)
	public Follow followUser(
		@org.springframework.security.core.annotation.AuthenticationPrincipal CurrentUser currentUser,
		@PathVariable UUID userId
	) {
		UUID followerId = currentUser.userId();
		if (followerId.equals(userId)) {
			throw new RuntimeException("Cannot follow self");
		}

		UserProfileRecord targetProfile = userProfileMapper.findFull(userId)
			.orElseThrow(() -> new RuntimeException("Target user profile not found"));

		String status = "ACTIVE";
		if (targetProfile.profileVisibility() == UserProfileVisibility.PRIVATE) {
			status = "PENDING";
		}

		UserFollowRecord existing = userFollowMapper.find(followerId, userId).orElse(null);
		Instant now = Instant.now();
		if (existing != null) {
			userFollowMapper.updateStatus(followerId, userId, status, now);
		} else {
			userFollowMapper.insert(followerId, userId, status, now, now);
		}

		return new Follow(
			followerId,
			userId,
			FollowStatus.valueOf(status),
			OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
		);
	}

	@DeleteMapping("/users/{userId}/follow")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unfollowUser(
		@org.springframework.security.core.annotation.AuthenticationPrincipal CurrentUser currentUser,
		@PathVariable UUID userId
	) {
		UUID followerId = currentUser.userId();
		userFollowMapper.find(followerId, userId)
			.orElseThrow(() -> new RuntimeException("Follow relationship not found"));

		userFollowMapper.updateStatus(followerId, userId, "DELETED", Instant.now());
	}

	@GetMapping("/users/{userId}/followers")
	public List<UserSummary> getFollowers(@PathVariable UUID userId) {
		List<UserSummaryRecord> records = userFollowMapper.findFollowers(userId);
		List<UserSummary> summaries = new ArrayList<>();
		for (UserSummaryRecord r : records) {
			summaries.add(new UserSummary(
				r.id(),
				r.displayName(),
				r.profileImageUrl() != null ? URI.create(r.profileImageUrl()) : null
			));
		}
		return summaries;
	}

	@GetMapping("/users/{userId}/following")
	public List<UserSummary> getFollowing(@PathVariable UUID userId) {
		List<UserSummaryRecord> records = userFollowMapper.findFollowing(userId);
		List<UserSummary> summaries = new ArrayList<>();
		for (UserSummaryRecord r : records) {
			summaries.add(new UserSummary(
				r.id(),
				r.displayName(),
				r.profileImageUrl() != null ? URI.create(r.profileImageUrl()) : null
			));
		}
		return summaries;
	}

	@GetMapping("/me/follow-requests")
	public PagedFollowRequest listReceivedFollowRequests(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@PatchMapping("/me/follow-requests/{followerUserId}/accept")
	public Follow acceptFollowRequest(@PathVariable UUID followerUserId) {
		return notImplemented();
	}

	@PatchMapping("/me/follow-requests/{followerUserId}/reject")
	public FollowRequest rejectFollowRequest(@PathVariable UUID followerUserId) {
		return notImplemented();
	}
}
