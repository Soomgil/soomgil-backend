package com.soomgil.community.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.CreateContentReportRequest;
import com.soomgil.community.api.dto.ReportReason;
import com.soomgil.community.application.command.CreateContentReportCommand;
import com.soomgil.community.application.handler.CreateContentReportCommandHandler;
import com.soomgil.community.application.handler.ListReportReasonsQueryHandler;
import com.soomgil.community.application.query.ListReportReasonsQuery;
import com.soomgil.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 커뮤니티 신고 REST 엔드포인트.
 *
 * <p>인증된 사용자가 게시글 또는 댓글을 신고할 수 있다.
 * 신고 사유 목록 조회는 비로그인도 가능하다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/community/reports")
@SecurityRequirement(name = "bearerAuth")
public class CommunityReportController extends ApiControllerSupport {

	private final ListReportReasonsQueryHandler listReportReasonsQueryHandler;
	private final CreateContentReportCommandHandler createContentReportCommandHandler;

	public CommunityReportController(
		ListReportReasonsQueryHandler listReportReasonsQueryHandler,
		CreateContentReportCommandHandler createContentReportCommandHandler
	) {
		this.listReportReasonsQueryHandler = listReportReasonsQueryHandler;
		this.createContentReportCommandHandler = createContentReportCommandHandler;
	}

	@GetMapping("/reasons")
	public List<ReportReason> listReportReasons() {
		return listReportReasonsQueryHandler.handle(new ListReportReasonsQuery());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ContentReport createReport(
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody CreateContentReportRequest request
	) {
		return createContentReportCommandHandler.handle(new CreateContentReportCommand(
			currentUser.userId(),
			request.targetType(),
			request.targetId(),
			request.reasonCode(),
			request.detail()
		));
	}
}
