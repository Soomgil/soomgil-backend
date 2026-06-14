package com.soomgil.community.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.CreateModerationActionRequest;
import com.soomgil.community.api.dto.ModerationAction;
import com.soomgil.community.api.dto.PagedContentReport;
import com.soomgil.community.api.dto.PagedModerationAction;
import com.soomgil.community.api.dto.ReportStatus;
import com.soomgil.community.api.dto.ResolveReportRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

@Validated
@RestController
@RequestMapping("/api/v1/moderation")
public class ModerationController extends ApiControllerSupport {

	@GetMapping("/reports")
	public PagedContentReport listReports(
		@RequestParam(required = false) ReportStatus status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@PatchMapping("/reports/{reportId}")
	public ContentReport resolveReport(
		@PathVariable UUID reportId,
		@Valid @RequestBody ResolveReportRequest request
	) {
		return notImplemented();
	}

	@GetMapping("/actions")
	public PagedModerationAction listActions(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@PostMapping("/actions")
	@ResponseStatus(HttpStatus.CREATED)
	public ModerationAction createAction(@Valid @RequestBody CreateModerationActionRequest request) {
		return notImplemented();
	}
}
