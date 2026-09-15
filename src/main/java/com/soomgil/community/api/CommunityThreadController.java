package com.soomgil.community.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.community.api.dto.CommunityThread;
import com.soomgil.community.api.dto.CommunityThreadReactionSummary;
import com.soomgil.community.api.dto.CommunityThreadReply;
import com.soomgil.community.api.dto.CreateCommunityThreadReplyRequest;
import com.soomgil.community.api.dto.CreateCommunityThreadRequest;
import com.soomgil.community.api.dto.PagedCommunityThread;
import com.soomgil.community.api.dto.PagedCommunityThreadReply;
import com.soomgil.community.api.dto.UpdateCommunityThreadReplyRequest;
import com.soomgil.community.api.dto.UpdateCommunityThreadRequest;
import com.soomgil.community.application.command.CreateCommunityThreadCommand;
import com.soomgil.community.application.command.CreateCommunityThreadReplyCommand;
import com.soomgil.community.application.command.DeleteCommunityThreadCommand;
import com.soomgil.community.application.command.DeleteCommunityThreadReplyCommand;
import com.soomgil.community.application.command.LikeCommunityThreadCommand;
import com.soomgil.community.application.command.UnlikeCommunityThreadCommand;
import com.soomgil.community.application.command.UpdateCommunityThreadCommand;
import com.soomgil.community.application.command.UpdateCommunityThreadReplyCommand;
import com.soomgil.community.application.handler.CreateCommunityThreadCommandHandler;
import com.soomgil.community.application.handler.CreateCommunityThreadReplyCommandHandler;
import com.soomgil.community.application.handler.DeleteCommunityThreadCommandHandler;
import com.soomgil.community.application.handler.DeleteCommunityThreadReplyCommandHandler;
import com.soomgil.community.application.handler.GetCommunityThreadQueryHandler;
import com.soomgil.community.application.handler.LikeCommunityThreadCommandHandler;
import com.soomgil.community.application.handler.ListCommunityThreadRepliesQueryHandler;
import com.soomgil.community.application.handler.ListCommunityThreadsQueryHandler;
import com.soomgil.community.application.handler.UnlikeCommunityThreadCommandHandler;
import com.soomgil.community.application.handler.UpdateCommunityThreadReplyCommandHandler;
import com.soomgil.community.application.handler.UpdateCommunityThreadCommandHandler;
import com.soomgil.community.application.query.GetCommunityThreadQuery;
import com.soomgil.community.application.query.ListCommunityThreadRepliesQuery;
import com.soomgil.community.application.query.ListCommunityThreadsQuery;
import com.soomgil.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 커뮤니티 공개 피드(쓰레드) REST 엔드포인트.
 *
 * <p>목록/상세/답글 조회는 공개이며 비로그인 요청에서는 {@code likedByMe}와 {@code editableByMe}가 false다.
 * 작성/수정/삭제/좋아요/답글은 인증이 필요하고, 수정과 삭제는 작성자만 수행할 수 있다.
 * 좋아요는 멱등하도록 {@code PUT}/{@code DELETE}를 사용한다.
 *
 * <p>기존 여행 스냅샷 게시글 API({@code /community/posts})는 deprecated 상태로 유지되며 신규 피드는
 * 이 컨트롤러만 사용한다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/community/threads")
@SecurityRequirement(name = "bearerAuth")
public class CommunityThreadController extends ApiControllerSupport {

	private final CreateCommunityThreadCommandHandler createCommunityThreadCommandHandler;
	private final UpdateCommunityThreadCommandHandler updateCommunityThreadCommandHandler;
	private final DeleteCommunityThreadCommandHandler deleteCommunityThreadCommandHandler;
	private final LikeCommunityThreadCommandHandler likeCommunityThreadCommandHandler;
	private final UnlikeCommunityThreadCommandHandler unlikeCommunityThreadCommandHandler;
	private final CreateCommunityThreadReplyCommandHandler createCommunityThreadReplyCommandHandler;
	private final UpdateCommunityThreadReplyCommandHandler updateCommunityThreadReplyCommandHandler;
	private final DeleteCommunityThreadReplyCommandHandler deleteCommunityThreadReplyCommandHandler;
	private final ListCommunityThreadsQueryHandler listCommunityThreadsQueryHandler;
	private final GetCommunityThreadQueryHandler getCommunityThreadQueryHandler;
	private final ListCommunityThreadRepliesQueryHandler listCommunityThreadRepliesQueryHandler;

	public CommunityThreadController(
		CreateCommunityThreadCommandHandler createCommunityThreadCommandHandler,
		UpdateCommunityThreadCommandHandler updateCommunityThreadCommandHandler,
		DeleteCommunityThreadCommandHandler deleteCommunityThreadCommandHandler,
		LikeCommunityThreadCommandHandler likeCommunityThreadCommandHandler,
		UnlikeCommunityThreadCommandHandler unlikeCommunityThreadCommandHandler,
		CreateCommunityThreadReplyCommandHandler createCommunityThreadReplyCommandHandler,
		UpdateCommunityThreadReplyCommandHandler updateCommunityThreadReplyCommandHandler,
		DeleteCommunityThreadReplyCommandHandler deleteCommunityThreadReplyCommandHandler,
		ListCommunityThreadsQueryHandler listCommunityThreadsQueryHandler,
		GetCommunityThreadQueryHandler getCommunityThreadQueryHandler,
		ListCommunityThreadRepliesQueryHandler listCommunityThreadRepliesQueryHandler
	) {
		this.createCommunityThreadCommandHandler = createCommunityThreadCommandHandler;
		this.updateCommunityThreadCommandHandler = updateCommunityThreadCommandHandler;
		this.deleteCommunityThreadCommandHandler = deleteCommunityThreadCommandHandler;
		this.likeCommunityThreadCommandHandler = likeCommunityThreadCommandHandler;
		this.unlikeCommunityThreadCommandHandler = unlikeCommunityThreadCommandHandler;
		this.createCommunityThreadReplyCommandHandler = createCommunityThreadReplyCommandHandler;
		this.updateCommunityThreadReplyCommandHandler = updateCommunityThreadReplyCommandHandler;
		this.deleteCommunityThreadReplyCommandHandler = deleteCommunityThreadReplyCommandHandler;
		this.listCommunityThreadsQueryHandler = listCommunityThreadsQueryHandler;
		this.getCommunityThreadQueryHandler = getCommunityThreadQueryHandler;
		this.listCommunityThreadRepliesQueryHandler = listCommunityThreadRepliesQueryHandler;
	}

	/**
	 * 공개 피드를 최신순으로 조회한다.
	 *
	 * @param currentUser 인증 사용자. 비로그인 요청이면 null
	 * @param authorId 특정 작성자만 조회할 때 사용
	 * @param query 본문 검색어
	 * @param page 0 기반 page 번호
	 * @param size page 크기. 최대 100으로 보정된다
	 * @return 쓰레드 page
	 */
	@GetMapping
	public PagedCommunityThread listThreads(
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(required = false) UUID authorId,
		@RequestParam(required = false) String query,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return listCommunityThreadsQueryHandler.handle(new ListCommunityThreadsQuery(
			viewerUserId(currentUser), authorId, query, page, size
		));
	}

	/**
	 * 쓰레드를 작성한다.
	 *
	 * @param currentUser 인증 사용자
	 * @param request 본문과 첨부 미디어 목록
	 * @return 생성된 쓰레드
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CommunityThread createThread(
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody CreateCommunityThreadRequest request
	) {
		return createCommunityThreadCommandHandler.handle(new CreateCommunityThreadCommand(
			currentUser.userId(), request.content(), request.mediaFileIds()
		));
	}

	/**
	 * 쓰레드 상세를 조회한다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param currentUser 인증 사용자. 비로그인 요청이면 null
	 * @return 쓰레드 상세
	 */
	@GetMapping("/{threadId}")
	public CommunityThread getThread(
		@PathVariable UUID threadId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return getCommunityThreadQueryHandler.handle(
			new GetCommunityThreadQuery(threadId, viewerUserId(currentUser))
		);
	}

	/**
	 * 쓰레드를 수정한다. 작성자만 호출할 수 있다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param currentUser 인증 사용자
	 * @param request 새 본문과 교체할 첨부 미디어 목록
	 * @return 수정된 쓰레드
	 */
	@PatchMapping("/{threadId}")
	public CommunityThread updateThread(
		@PathVariable UUID threadId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody UpdateCommunityThreadRequest request
	) {
		return updateCommunityThreadCommandHandler.handle(new UpdateCommunityThreadCommand(
			threadId, currentUser.userId(), request.content(), request.mediaFileIds()
		));
	}

	/**
	 * 쓰레드를 삭제한다. 작성자만 호출할 수 있고 soft delete로 처리한다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param currentUser 인증 사용자
	 */
	@DeleteMapping("/{threadId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteThread(
		@PathVariable UUID threadId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		deleteCommunityThreadCommandHandler.handle(
			new DeleteCommunityThreadCommand(threadId, currentUser.userId())
		);
	}

	/**
	 * 쓰레드에 좋아요를 남긴다. 여러 번 호출해도 결과가 같은 멱등 연산이다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param currentUser 인증 사용자
	 * @return 처리 후 좋아요 요약
	 */
	@PutMapping("/{threadId}/like")
	public CommunityThreadReactionSummary likeThread(
		@PathVariable UUID threadId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return likeCommunityThreadCommandHandler.handle(
			new LikeCommunityThreadCommand(threadId, currentUser.userId())
		);
	}

	/**
	 * 쓰레드 좋아요를 취소한다. 좋아요가 없어도 실패하지 않는 멱등 연산이다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param currentUser 인증 사용자
	 * @return 처리 후 좋아요 요약
	 */
	@DeleteMapping("/{threadId}/like")
	public CommunityThreadReactionSummary unlikeThread(
		@PathVariable UUID threadId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return unlikeCommunityThreadCommandHandler.handle(
			new UnlikeCommunityThreadCommand(threadId, currentUser.userId())
		);
	}

	/**
	 * 쓰레드 답글 목록을 조회한다. page 단위는 root 답글이다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param currentUser 인증 사용자. 비로그인 요청이면 null
	 * @param page 0 기반 page 번호
	 * @param size page 크기
	 * @return 답글 page
	 */
	@GetMapping("/{threadId}/replies")
	public PagedCommunityThreadReply listReplies(
		@PathVariable UUID threadId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return listCommunityThreadRepliesQueryHandler.handle(
			new ListCommunityThreadRepliesQuery(threadId, viewerUserId(currentUser), page, size)
		);
	}

	/**
	 * 답글을 작성한다. {@code parentReplyId}를 지정하면 1단계 하위 답글이 된다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param currentUser 인증 사용자
	 * @param request 부모 답글 식별자와 본문
	 * @return 생성된 답글
	 */
	@PostMapping("/{threadId}/replies")
	@ResponseStatus(HttpStatus.CREATED)
	public CommunityThreadReply createReply(
		@PathVariable UUID threadId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody CreateCommunityThreadReplyRequest request
	) {
		return createCommunityThreadReplyCommandHandler.handle(new CreateCommunityThreadReplyCommand(
			threadId, currentUser.userId(), request.parentReplyId(), request.content()
		));
	}

	/**
	 * 답글을 수정한다. 답글 작성자만 호출할 수 있다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param replyId 답글 식별자
	 * @param currentUser 인증 사용자
	 * @param request 새 본문
	 * @return 수정된 답글
	 */
	@PatchMapping("/{threadId}/replies/{replyId}")
	public CommunityThreadReply updateReply(
		@PathVariable UUID threadId,
		@PathVariable UUID replyId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody UpdateCommunityThreadReplyRequest request
	) {
		return updateCommunityThreadReplyCommandHandler.handle(new UpdateCommunityThreadReplyCommand(
			threadId, replyId, currentUser.userId(), request.content()
		));
	}

	/**
	 * 답글을 삭제한다. 답글 작성자와 쓰레드 작성자가 호출할 수 있다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param replyId 답글 식별자
	 * @param currentUser 인증 사용자
	 */
	@DeleteMapping("/{threadId}/replies/{replyId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteReply(
		@PathVariable UUID threadId,
		@PathVariable UUID replyId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		deleteCommunityThreadReplyCommandHandler.handle(
			new DeleteCommunityThreadReplyCommand(threadId, replyId, currentUser.userId())
		);
	}

	private UUID viewerUserId(CurrentUser currentUser) {
		return currentUser == null ? null : currentUser.userId();
	}
}
