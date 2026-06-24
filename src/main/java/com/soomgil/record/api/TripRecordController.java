package com.soomgil.record.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.record.api.dto.CreateTripRecordRequest;
import com.soomgil.record.api.dto.PagedTripRecordEntry;
import com.soomgil.record.api.dto.PagedTripRecordPhoto;
import com.soomgil.record.api.dto.TripRecordEntry;
import com.soomgil.record.api.dto.TripRecordDay;
import com.soomgil.record.api.dto.UpdateTripRecordRequest;
import com.soomgil.record.application.handler.TripRecordService;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/records")
public class TripRecordController extends ApiControllerSupport {

	private final TripRecordService tripRecordService;

	public TripRecordController(TripRecordService tripRecordService) {
		this.tripRecordService = Objects.requireNonNull(tripRecordService, "tripRecordService must not be null");
	}

	@GetMapping
	public PagedTripRecordEntry listRecords(
		@PathVariable UUID tripId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort,
		Principal principal
	) {
		return tripRecordService.listRecords(tripId, currentUserId(principal), page, size, sort);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TripRecordEntry createRecord(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateTripRecordRequest request,
		@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
		Principal principal
	) {
		return tripRecordService.createRecord(tripId, currentUserId(principal), request, idempotencyKey);
	}

	@GetMapping("/{recordId}")
	public TripRecordEntry getRecord(
		@PathVariable UUID tripId,
		@PathVariable UUID recordId,
		Principal principal
	) {
		return tripRecordService.getRecord(tripId, currentUserId(principal), recordId);
	}

	@GetMapping("/days")
	public List<TripRecordDay> listRecordDays(
		@PathVariable UUID tripId,
		Principal principal
	) {
		return tripRecordService.listDays(tripId, currentUserId(principal));
	}

	@PatchMapping("/{recordId}")
	public TripRecordEntry updateRecord(
		@PathVariable UUID tripId,
		@PathVariable UUID recordId,
		@Valid @RequestBody UpdateTripRecordRequest request,
		Principal principal
	) {
		return tripRecordService.updateRecord(tripId, currentUserId(principal), recordId, request);
	}

	@DeleteMapping("/{recordId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteRecord(
		@PathVariable UUID tripId,
		@PathVariable UUID recordId,
		Principal principal
	) {
		tripRecordService.deleteRecord(tripId, currentUserId(principal), recordId);
	}

	@GetMapping("/photos")
	public PagedTripRecordPhoto listRecordPhotos(
		@PathVariable UUID tripId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort,
		Principal principal
	) {
		return tripRecordService.listPhotos(tripId, currentUserId(principal), page, size, sort);
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
