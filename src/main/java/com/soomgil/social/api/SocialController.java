package com.soomgil.social.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.social.api.dto.Follow;
import com.soomgil.social.api.dto.FollowRequest;
import com.soomgil.social.api.dto.PagedFollowRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SocialController extends ApiControllerSupport {

	@PostMapping("/users/{userId}/follow")
	@ResponseStatus(HttpStatus.CREATED)
	public Follow followUser(@PathVariable UUID userId) {
		return notImplemented();
	}

	@DeleteMapping("/users/{userId}/follow")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unfollowUser(@PathVariable UUID userId) {
		notImplemented();
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
