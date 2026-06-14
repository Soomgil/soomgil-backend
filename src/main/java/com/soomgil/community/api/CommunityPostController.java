package com.soomgil.community.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.community.api.dto.CommunityComment;
import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.api.dto.CommunityPostReactionSummary;
import com.soomgil.community.api.dto.CommunityPostShareTokenResponse;
import com.soomgil.community.api.dto.CreateCommunityCommentRequest;
import com.soomgil.community.api.dto.CreateCommunityPostRequest;
import com.soomgil.community.api.dto.PagedCommunityComment;
import com.soomgil.community.api.dto.PagedCommunityPostSummary;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.api.dto.RetripRequest;
import com.soomgil.community.api.dto.UpdateCommunityPostRequest;
import com.soomgil.trip.api.dto.TripDetail;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({"/api/v1/community/posts", "/api/v1/stories"})
public class CommunityPostController extends ApiControllerSupport {

	@GetMapping
	public PagedCommunityPostSummary listPosts(
		@RequestParam(required = false) String query,
		@RequestParam(required = false) String hashtag,
		@RequestParam(required = false) PostVisibility visibility,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CommunityPostDetail createPost(@Valid @RequestBody CreateCommunityPostRequest request) {
		return notImplemented();
	}

	@GetMapping("/{postId}")
	public CommunityPostDetail getPost(@PathVariable UUID postId) {
		return notImplemented();
	}

	@PatchMapping("/{postId}")
	public CommunityPostDetail updatePost(
		@PathVariable UUID postId,
		@Valid @RequestBody UpdateCommunityPostRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/{postId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePost(@PathVariable UUID postId) {
		notImplemented();
	}

	@PostMapping("/{postId}/likes")
	public CommunityPostReactionSummary likePost(@PathVariable UUID postId) {
		return notImplemented();
	}

	@DeleteMapping("/{postId}/likes")
	public CommunityPostReactionSummary unlikePost(@PathVariable UUID postId) {
		return notImplemented();
	}

	@PostMapping("/{postId}/retrip")
	@ResponseStatus(HttpStatus.CREATED)
	public TripDetail retrip(
		@PathVariable UUID postId,
		@Valid @RequestBody RetripRequest request
	) {
		return notImplemented();
	}

	@PostMapping("/{postId}/share-token")
	public CommunityPostShareTokenResponse rotateShareToken(@PathVariable UUID postId) {
		return notImplemented();
	}

	@GetMapping("/{postId}/comments")
	public PagedCommunityComment listComments(
		@PathVariable UUID postId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@PostMapping("/{postId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommunityComment createComment(
		@PathVariable UUID postId,
		@Valid @RequestBody CreateCommunityCommentRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/{postId}/comments/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteComment(@PathVariable UUID postId, @PathVariable UUID commentId) {
		notImplemented();
	}
}
