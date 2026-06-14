package com.soomgil.community.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.CreateContentReportRequest;
import com.soomgil.community.api.dto.ReportReason;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/community/reports")
public class CommunityReportController extends ApiControllerSupport {

	@GetMapping("/reasons")
	public List<ReportReason> listReportReasons() {
		return notImplemented();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ContentReport createReport(@Valid @RequestBody CreateContentReportRequest request) {
		return notImplemented();
	}
}
