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
import com.soomgil.community.application.command.CreateCommunityCommentCommand;
import com.soomgil.community.application.command.CreateCommunityPostCommand;
import com.soomgil.community.application.command.DeleteCommunityCommentCommand;
import com.soomgil.community.application.command.DeleteCommunityPostCommand;
import com.soomgil.community.application.command.LikePostCommand;
import com.soomgil.community.application.command.RotatePostShareTokenCommand;
import com.soomgil.community.application.command.UnlikePostCommand;
import com.soomgil.community.application.command.UpdateCommunityPostCommand;
import com.soomgil.community.application.handler.CreateCommunityCommentCommandHandler;
import com.soomgil.community.application.handler.CreateCommunityPostCommandHandler;
import com.soomgil.community.application.handler.DeleteCommunityCommentCommandHandler;
import com.soomgil.community.application.handler.DeleteCommunityPostCommandHandler;
import com.soomgil.community.application.handler.GetCommunityPostQueryHandler;
import com.soomgil.community.application.handler.LikePostCommandHandler;
import com.soomgil.community.application.handler.ListCommentsQueryHandler;
import com.soomgil.community.application.handler.ListCommunityPostsQueryHandler;
import com.soomgil.community.application.handler.RotatePostShareTokenCommandHandler;
import com.soomgil.community.application.handler.UnlikePostCommandHandler;
import com.soomgil.community.application.handler.UpdateCommunityPostCommandHandler;
import com.soomgil.community.application.service.RetripCommunityPostService;
import com.soomgil.community.application.query.GetCommunityPostQuery;
import com.soomgil.community.application.query.ListCommentsQuery;
import com.soomgil.community.application.query.ListCommunityPostsQuery;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.trip.api.dto.TripDetail;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

/**
 * 커뮤니티 게시글 REST 엔드포인트.
 *
 * <p>Phase 1: 게시글 CRUD + 공개 feed + 공유 토큰 rotate.
 * Phase 2: 좋아요, 댓글 CRUD.
 * Retrip은 저장된 게시글 snapshot을 독립된 새 여행방으로 복제한다.
 */
@Validated
@RestController
@RequestMapping({"/api/v1/community/posts", "/api/v1/stories"})
@SecurityRequirement(name = "bearerAuth")
public class CommunityPostController extends ApiControllerSupport {

	private final CreateCommunityPostCommandHandler createCommunityPostCommandHandler;
	private final UpdateCommunityPostCommandHandler updateCommunityPostCommandHandler;
	private final DeleteCommunityPostCommandHandler deleteCommunityPostCommandHandler;
	private final RotatePostShareTokenCommandHandler rotatePostShareTokenCommandHandler;
	private final GetCommunityPostQueryHandler getCommunityPostQueryHandler;
	private final ListCommunityPostsQueryHandler listCommunityPostsQueryHandler;
	private final LikePostCommandHandler likePostCommandHandler;
	private final UnlikePostCommandHandler unlikePostCommandHandler;
	private final CreateCommunityCommentCommandHandler createCommunityCommentCommandHandler;
	private final DeleteCommunityCommentCommandHandler deleteCommunityCommentCommandHandler;
	private final ListCommentsQueryHandler listCommentsQueryHandler;
	private final RetripCommunityPostService retripCommunityPostService;

	public CommunityPostController(
		CreateCommunityPostCommandHandler createCommunityPostCommandHandler,
		UpdateCommunityPostCommandHandler updateCommunityPostCommandHandler,
		DeleteCommunityPostCommandHandler deleteCommunityPostCommandHandler,
		RotatePostShareTokenCommandHandler rotatePostShareTokenCommandHandler,
		GetCommunityPostQueryHandler getCommunityPostQueryHandler,
		ListCommunityPostsQueryHandler listCommunityPostsQueryHandler,
		LikePostCommandHandler likePostCommandHandler,
		UnlikePostCommandHandler unlikePostCommandHandler,
		CreateCommunityCommentCommandHandler createCommunityCommentCommandHandler,
		DeleteCommunityCommentCommandHandler deleteCommunityCommentCommandHandler,
		ListCommentsQueryHandler listCommentsQueryHandler,
		RetripCommunityPostService retripCommunityPostService
	) {
		this.createCommunityPostCommandHandler = createCommunityPostCommandHandler;
		this.updateCommunityPostCommandHandler = updateCommunityPostCommandHandler;
		this.deleteCommunityPostCommandHandler = deleteCommunityPostCommandHandler;
		this.rotatePostShareTokenCommandHandler = rotatePostShareTokenCommandHandler;
		this.getCommunityPostQueryHandler = getCommunityPostQueryHandler;
		this.listCommunityPostsQueryHandler = listCommunityPostsQueryHandler;
		this.likePostCommandHandler = likePostCommandHandler;
		this.unlikePostCommandHandler = unlikePostCommandHandler;
		this.createCommunityCommentCommandHandler = createCommunityCommentCommandHandler;
		this.deleteCommunityCommentCommandHandler = deleteCommunityCommentCommandHandler;
		this.listCommentsQueryHandler = listCommentsQueryHandler;
		this.retripCommunityPostService = retripCommunityPostService;
	}

	@GetMapping
	public PagedCommunityPostSummary listPosts(
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(required = false) String query,
		@RequestParam(required = false) String hashtag,
		@RequestParam(required = false) PostVisibility visibility,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		UUID viewerUserId = currentUser != null ? currentUser.userId() : null;
		return listCommunityPostsQueryHandler.handle(
			new ListCommunityPostsQuery(null, visibility, page, size, viewerUserId)
		);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CommunityPostDetail createPost(
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody CreateCommunityPostRequest request
	) {
		return createCommunityPostCommandHandler.handle(new CreateCommunityPostCommand(
			request.sourceTripId(),
			request.baseVersion(),
			currentUser.userId(),
			request.visibility(),
			request.title(),
			request.summary(),
			request.coverMediaFileId(),
			request.mediaFileIds(),
			request.hashtags()
		));
	}

	@GetMapping("/{postId}")
	public CommunityPostDetail getPost(
		@PathVariable UUID postId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(name = "share", required = false) String shareToken
	) {
		UUID viewerUserId = currentUser != null ? currentUser.userId() : null;
		return getCommunityPostQueryHandler.handle(
			new GetCommunityPostQuery(postId, viewerUserId, shareToken)
		);
	}

	@PatchMapping("/{postId}")
	public CommunityPostDetail updatePost(
		@PathVariable UUID postId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody UpdateCommunityPostRequest request
	) {
		return updateCommunityPostCommandHandler.handle(new UpdateCommunityPostCommand(
			postId,
			currentUser.userId(),
			request.title(),
			request.summary(),
			request.visibility(),
			request.coverMediaFileId(),
			request.mediaFileIds(),
			request.hashtags()
		));
	}

	@DeleteMapping("/{postId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePost(
		@PathVariable UUID postId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		deleteCommunityPostCommandHandler.handle(
			new DeleteCommunityPostCommand(postId, currentUser.userId(), null)
		);
	}

	@PostMapping("/{postId}/likes")
	public CommunityPostReactionSummary likePost(
		@PathVariable UUID postId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return likePostCommandHandler.handle(
			new LikePostCommand(postId, currentUser.userId())
		);
	}

	@DeleteMapping("/{postId}/likes")
	public CommunityPostReactionSummary unlikePost(
		@PathVariable UUID postId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return unlikePostCommandHandler.handle(
			new UnlikePostCommand(postId, currentUser.userId())
		);
	}

	@PostMapping("/{postId}/retrip")
	@ResponseStatus(HttpStatus.CREATED)
	public TripDetail retrip(
		@PathVariable UUID postId,
		@Valid @RequestBody(required = false) RetripRequest request,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return retripCommunityPostService.retrip(
			postId, currentUser.userId(), request == null ? null : request.title()
		);
	}

	@PostMapping("/{postId}/share-token")
	public CommunityPostShareTokenResponse rotateShareToken(
		@PathVariable UUID postId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return rotatePostShareTokenCommandHandler.handle(
			new RotatePostShareTokenCommand(postId, currentUser.userId())
		);
	}

	@GetMapping("/{postId}/comments")
	public PagedCommunityComment listComments(
		@PathVariable UUID postId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return listCommentsQueryHandler.handle(
			new ListCommentsQuery(postId, page, size)
		);
	}

	@PostMapping("/{postId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommunityComment createComment(
		@PathVariable UUID postId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody CreateCommunityCommentRequest request
	) {
		return createCommunityCommentCommandHandler.handle(new CreateCommunityCommentCommand(
			postId,
			currentUser.userId(),
			request.parentCommentId(),
			request.content()
		));
	}

	@DeleteMapping("/{postId}/comments/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteComment(
		@PathVariable UUID postId,
		@PathVariable UUID commentId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		deleteCommunityCommentCommandHandler.handle(
			new DeleteCommunityCommentCommand(postId, commentId, currentUser.userId())
		);
	}
}
