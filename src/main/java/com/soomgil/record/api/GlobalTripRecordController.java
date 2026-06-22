package com.soomgil.record.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.record.api.dto.PagedTripRecordPhoto;
import com.soomgil.record.api.dto.TripRecordPhotoSummaryRequest;
import com.soomgil.record.api.dto.TripRecordPhotoSummaryResponse;
import com.soomgil.record.api.dto.TripRecordPhotoReadUrl;
import com.soomgil.record.application.handler.TripRecordService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 사용자가 참여 중인 모든 여행방의 기록 조회 API.
 */
@Validated
@RestController
@RequestMapping("/api/v1/records")
public class GlobalTripRecordController extends ApiControllerSupport {

	private final TripRecordService tripRecordService;

	public GlobalTripRecordController(TripRecordService tripRecordService) {
		this.tripRecordService = Objects.requireNonNull(tripRecordService, "tripRecordService must not be null");
	}

	@GetMapping("/photos")
	public PagedTripRecordPhoto listRecordPhotos(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort,
		Principal principal
	) {
		return tripRecordService.listPhotos(currentUserId(principal), page, size, sort);
	}

	@GetMapping("/photos/{mediaFileId}/read-url")
	public TripRecordPhotoReadUrl refreshRecordPhotoReadUrl(
		@PathVariable UUID mediaFileId,
		Principal principal
	) {
		return tripRecordService.refreshPhotoReadUrl(currentUserId(principal), mediaFileId);
	}

	@PostMapping("/photo-summaries")
	public TripRecordPhotoSummaryResponse summarizeRecordPhotos(
		@Valid @RequestBody TripRecordPhotoSummaryRequest request,
		Principal principal
	) {
		return tripRecordService.summarizePhotos(currentUserId(principal), request.tripIds());
	}

	private UUID currentUserId(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authenticated user is required.");
		}
		try {
			return Ids.parseUuid(principal.getName(), "currentUserId");
		}
		catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authenticated user is invalid.");
		}
	}
}
