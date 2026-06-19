package com.soomgil.community.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.CreateModerationActionRequest;
import com.soomgil.community.api.dto.ModerationAction;
import com.soomgil.community.api.dto.PagedContentReport;
import com.soomgil.community.api.dto.PagedModerationAction;
import com.soomgil.community.api.dto.ReportStatus;
import com.soomgil.community.api.dto.ResolveReportRequest;
import com.soomgil.community.application.command.CreateModerationActionCommand;
import com.soomgil.community.application.command.ResolveReportCommand;
import com.soomgil.community.application.handler.CreateModerationActionCommandHandler;
import com.soomgil.community.application.handler.ListModerationActionsQueryHandler;
import com.soomgil.community.application.handler.ListReportsQueryHandler;
import com.soomgil.community.application.handler.ResolveReportCommandHandler;
import com.soomgil.community.application.query.ListModerationActionsQuery;
import com.soomgil.community.application.query.ListReportsQuery;
import com.soomgil.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
 * 모더레이션 REST 엔드포인트 (MODERATOR 전용).
 *
 * <p>모든 엔드포인트는 인증이 필요하며, handler에서 MODERATOR 역할을 추가로 검증한다.
 * 신고 목록 조회, 신고 처리, 조치 이력 조회, 직접 조치를 제공한다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/moderation")
@SecurityRequirement(name = "bearerAuth")
public class ModerationController extends ApiControllerSupport {

	private final ListReportsQueryHandler listReportsQueryHandler;
	private final ResolveReportCommandHandler resolveReportCommandHandler;
	private final ListModerationActionsQueryHandler listModerationActionsQueryHandler;
	private final CreateModerationActionCommandHandler createModerationActionCommandHandler;

	public ModerationController(
		ListReportsQueryHandler listReportsQueryHandler,
		ResolveReportCommandHandler resolveReportCommandHandler,
		ListModerationActionsQueryHandler listModerationActionsQueryHandler,
		CreateModerationActionCommandHandler createModerationActionCommandHandler
	) {
		this.listReportsQueryHandler = listReportsQueryHandler;
		this.resolveReportCommandHandler = resolveReportCommandHandler;
		this.listModerationActionsQueryHandler = listModerationActionsQueryHandler;
		this.createModerationActionCommandHandler = createModerationActionCommandHandler;
	}

	@GetMapping("/reports")
	public PagedContentReport listReports(
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(required = false) ReportStatus status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return listReportsQueryHandler.handle(new ListReportsQuery(status, page, size));
	}

	@PatchMapping("/reports/{reportId}")
	public ContentReport resolveReport(
		@PathVariable UUID reportId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody ResolveReportRequest request
	) {
		return resolveReportCommandHandler.handle(new ResolveReportCommand(
			reportId,
			currentUser.userId(),
			ReportStatus.valueOf(request.status()),
			request.resolutionNote(),
			request.moderationAction() != null ? request.moderationAction().action() : null,
			request.moderationAction() != null ? request.moderationAction().moderationReason() : null
		));
	}

	@GetMapping("/actions")
	public PagedModerationAction listActions(
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return listModerationActionsQueryHandler.handle(
			new ListModerationActionsQuery(page, size));
	}

	@PostMapping("/actions")
	@ResponseStatus(HttpStatus.CREATED)
	public ModerationAction createAction(
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody CreateModerationActionRequest request
	) {
		return createModerationActionCommandHandler.handle(new CreateModerationActionCommand(
			currentUser.userId(),
			request.targetType(),
			request.targetId(),
			request.action(),
			request.moderationReason(),
			null
		));
	}
}
