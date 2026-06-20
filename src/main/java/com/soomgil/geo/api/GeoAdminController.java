package com.soomgil.geo.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.geo.api.dto.SyncLegalRegionsResponse;
import com.soomgil.geo.application.command.dto.SyncLegalRegionsCommand;
import com.soomgil.geo.application.command.dto.SyncLegalRegionsResult;
import com.soomgil.geo.application.command.handler.SyncLegalRegionsHandler;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 관리자용 지리 데이터 적재 API.
 *
 * <p>법정동 CSV는 운영자가 검증한 정부/공공데이터 파일만 업로드해야 한다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class GeoAdminController extends ApiControllerSupport {

	private final SyncLegalRegionsHandler syncLegalRegionsHandler;

	public GeoAdminController(SyncLegalRegionsHandler syncLegalRegionsHandler) {
		this.syncLegalRegionsHandler = Objects.requireNonNull(
			syncLegalRegionsHandler,
			"syncLegalRegionsHandler must not be null"
		);
	}

	@PostMapping("/legal-regions/sync-runs")
	public SyncLegalRegionsResponse syncLegalRegions(
		@RequestParam MultipartFile file,
		@RequestParam(required = false) String source
	) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Legal region CSV file is required.");
		}

		SyncLegalRegionsResult result = syncLegalRegionsHandler.handle(new SyncLegalRegionsCommand(
			source,
			file.getOriginalFilename(),
			readBytes(file),
			StandardCharsets.UTF_8
		));
		return new SyncLegalRegionsResponse(
			result.totalCount(),
			result.insertedCount(),
			result.updatedCount(),
			result.deactivatedCount()
		);
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		}
		catch (IOException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "Failed to read legal region CSV file.");
		}
	}
}
