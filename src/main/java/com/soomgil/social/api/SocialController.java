package com.soomgil.social.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.social.api.dto.Follow;
import com.soomgil.social.api.dto.PagedFollowRequest;
import com.soomgil.social.application.SocialFollowService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SocialController extends ApiControllerSupport {

	private final SocialFollowService followService;

	public SocialController(SocialFollowService followService) {
		this.followService = followService;
	}

	@PutMapping("/users/{userId}/follow")
	public Follow followUser(@PathVariable UUID userId, Principal principal) {
		return followService.follow(currentUserId(principal), userId);
	}

	@DeleteMapping("/users/{userId}/follow")
	@ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
	public void unfollowUser(@PathVariable UUID userId, Principal principal) {
		followService.unfollow(currentUserId(principal), userId);
	}

	@GetMapping("/me/follow-requests")
	public PagedFollowRequest listReceivedFollowRequests(
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
		Principal principal
	) {
		return followService.listPending(currentUserId(principal), page, size);
	}

	@PutMapping("/me/follow-requests/{followerUserId}/accept")
	public Follow acceptFollowRequest(@PathVariable UUID followerUserId, Principal principal) {
		return followService.accept(currentUserId(principal), followerUserId);
	}

	@DeleteMapping("/me/follow-requests/{followerUserId}")
	@ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
	public void rejectFollowRequest(@PathVariable UUID followerUserId, Principal principal) {
		followService.reject(currentUserId(principal), followerUserId);
	}

	private UUID currentUserId(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		try {
			return Ids.parseUuid(principal.getName(), "currentUserId");
		}
		catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}
}
