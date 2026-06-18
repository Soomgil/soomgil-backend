package com.soomgil.user.api;

import com.soomgil.auth.application.handler.GetCurrentUserQueryHandler;
import com.soomgil.auth.application.handler.ListSecurityEventsQueryHandler;
import com.soomgil.auth.application.handler.ListSessionsQueryHandler;
import com.soomgil.auth.application.handler.RevokeSessionCommandHandler;
import com.soomgil.auth.application.query.GetCurrentUserQuery;
import com.soomgil.auth.application.query.ListSecurityEventsQuery;
import com.soomgil.auth.application.query.ListSessionsQuery;
import com.soomgil.auth.api.dto.PagedSecurityEvent;
import com.soomgil.auth.api.dto.PagedUserSession;
import com.soomgil.auth.application.command.RevokeSessionCommand;
import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.user.api.dto.PagedUserSummary;
import com.soomgil.user.api.dto.UpdateMeRequest;
import com.soomgil.user.api.dto.UpdateUserSettingsRequest;
import com.soomgil.user.api.dto.User;
import com.soomgil.user.api.dto.UserPublicProfile;
import com.soomgil.user.api.dto.UserSettings;
import com.soomgil.user.application.command.RequestAccountDeletionCommand;
import com.soomgil.user.application.command.UpdateMeCommand;
import com.soomgil.user.application.command.UpdateMySettingsCommand;
import com.soomgil.user.application.handler.GetMySettingsQueryHandler;
import com.soomgil.user.application.handler.GetUserPublicProfileQueryHandler;
import com.soomgil.user.application.handler.RequestAccountDeletionCommandHandler;
import com.soomgil.user.application.handler.SearchUsersQueryHandler;
import com.soomgil.user.application.handler.UpdateMeCommandHandler;
import com.soomgil.user.application.handler.UpdateMySettingsCommandHandler;
import com.soomgil.user.application.query.GetUserPublicProfileQuery;
import com.soomgil.user.application.query.SearchUsersQuery;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 사용자 도메인의 REST 엔드포인트.
 *
 * <p>{@code /me} 계열(프로필, 설정, 계정 삭제 예약, 세션, 보안 이벤트)과
 * {@code /users} 계열(검색, 공개 프로필)을 담당한다. 인증/세션/토큰 발급 자체은
 * {@code AuthController}({@code /api/v1/auth/*})에서 다룬다.
 */
@Validated
@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class UserController extends ApiControllerSupport {

	private final GetCurrentUserQueryHandler getCurrentUserQueryHandler;
	private final UpdateMeCommandHandler updateMeCommandHandler;
	private final RequestAccountDeletionCommandHandler requestAccountDeletionCommandHandler;
	private final GetMySettingsQueryHandler getMySettingsQueryHandler;
	private final UpdateMySettingsCommandHandler updateMySettingsCommandHandler;
	private final SearchUsersQueryHandler searchUsersQueryHandler;
	private final GetUserPublicProfileQueryHandler getUserPublicProfileQueryHandler;
	private final ListSessionsQueryHandler listSessionsQueryHandler;
	private final RevokeSessionCommandHandler revokeSessionCommandHandler;
	private final ListSecurityEventsQueryHandler listSecurityEventsQueryHandler;

	public UserController(
		GetCurrentUserQueryHandler getCurrentUserQueryHandler,
		UpdateMeCommandHandler updateMeCommandHandler,
		RequestAccountDeletionCommandHandler requestAccountDeletionCommandHandler,
		GetMySettingsQueryHandler getMySettingsQueryHandler,
		UpdateMySettingsCommandHandler updateMySettingsCommandHandler,
		SearchUsersQueryHandler searchUsersQueryHandler,
		GetUserPublicProfileQueryHandler getUserPublicProfileQueryHandler,
		ListSessionsQueryHandler listSessionsQueryHandler,
		RevokeSessionCommandHandler revokeSessionCommandHandler,
		ListSecurityEventsQueryHandler listSecurityEventsQueryHandler
	) {
		this.getCurrentUserQueryHandler = getCurrentUserQueryHandler;
		this.updateMeCommandHandler = updateMeCommandHandler;
		this.requestAccountDeletionCommandHandler = requestAccountDeletionCommandHandler;
		this.getMySettingsQueryHandler = getMySettingsQueryHandler;
		this.updateMySettingsCommandHandler = updateMySettingsCommandHandler;
		this.searchUsersQueryHandler = searchUsersQueryHandler;
		this.getUserPublicProfileQueryHandler = getUserPublicProfileQueryHandler;
		this.listSessionsQueryHandler = listSessionsQueryHandler;
		this.revokeSessionCommandHandler = revokeSessionCommandHandler;
		this.listSecurityEventsQueryHandler = listSecurityEventsQueryHandler;
	}

	@GetMapping("/me")
	public User getMe(@AuthenticationPrincipal CurrentUser currentUser) {
		return getCurrentUserQueryHandler.handle(new GetCurrentUserQuery(currentUser.userId()));
	}

	@PatchMapping("/me")
	public User updateMe(
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody UpdateMeRequest request
	) {
		return updateMeCommandHandler.handle(new UpdateMeCommand(
			currentUser.userId(),
			request.displayName(),
			request.profileMediaFileId(),
			request.bio(),
			request.profileVisibility()
		));
	}

	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void deleteMe(@AuthenticationPrincipal CurrentUser currentUser) {
		requestAccountDeletionCommandHandler.handle(
			new RequestAccountDeletionCommand(currentUser.userId())
		);
	}

	@GetMapping("/me/settings")
	public UserSettings getMySettings(@AuthenticationPrincipal CurrentUser currentUser) {
		return getMySettingsQueryHandler.handle(
			new com.soomgil.user.application.query.GetMySettingsQuery(currentUser.userId())
		);
	}

	@PatchMapping("/me/settings")
	public UserSettings updateMySettings(
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody UpdateUserSettingsRequest request
	) {
		return updateMySettingsCommandHandler.handle(new UpdateMySettingsCommand(
			currentUser.userId(),
			request.displayLanguage(),
			request.timezone(),
			request.marketingEmailOptIn(),
			request.tripInviteEmailOptIn()
		));
	}

	@GetMapping("/me/sessions")
	public PagedUserSession listMySessions(
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return listSessionsQueryHandler.handle(
			new ListSessionsQuery(currentUser.userId(), page, size)
		);
	}

	@DeleteMapping("/me/sessions/{sessionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revokeMySession(
		@AuthenticationPrincipal CurrentUser currentUser,
		@PathVariable UUID sessionId
	) {
		revokeSessionCommandHandler.handle(
			new RevokeSessionCommand(sessionId, currentUser.userId())
		);
	}

	@GetMapping("/me/security-events")
	public PagedSecurityEvent listMySecurityEvents(
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return listSecurityEventsQueryHandler.handle(
			new ListSecurityEventsQuery(currentUser.userId(), page, size)
		);
	}

	@GetMapping("/users")
	public PagedUserSummary searchUsers(
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(name = "q", required = false) String query,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return searchUsersQueryHandler.handle(new SearchUsersQuery(query, page, size));
	}

	@GetMapping("/users/{userId}")
	public UserPublicProfile getUserProfile(
		@AuthenticationPrincipal CurrentUser currentUser,
		@PathVariable UUID userId
	) {
		return getUserPublicProfileQueryHandler.handle(
			new GetUserPublicProfileQuery(currentUser.userId(), userId)
		);
	}
}
