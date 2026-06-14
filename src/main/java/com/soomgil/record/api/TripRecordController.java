package com.soomgil.record.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.record.api.dto.CreateTripRecordRequest;
import com.soomgil.record.api.dto.PagedTripRecordEntry;
import com.soomgil.record.api.dto.PagedTripRecordPhoto;
import com.soomgil.record.api.dto.TripRecordEntry;
import com.soomgil.record.api.dto.UpdateTripRecordRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/records")
public class TripRecordController extends ApiControllerSupport {

	@GetMapping
	public PagedTripRecordEntry listRecords(
		@PathVariable UUID tripId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TripRecordEntry createRecord(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateTripRecordRequest request
	) {
		return notImplemented();
	}

	@GetMapping("/{recordId}")
	public TripRecordEntry getRecord(@PathVariable UUID tripId, @PathVariable UUID recordId) {
		return notImplemented();
	}

	@PatchMapping("/{recordId}")
	public TripRecordEntry updateRecord(
		@PathVariable UUID tripId,
		@PathVariable UUID recordId,
		@Valid @RequestBody UpdateTripRecordRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/{recordId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteRecord(@PathVariable UUID tripId, @PathVariable UUID recordId) {
		notImplemented();
	}

	@GetMapping("/photos")
	public PagedTripRecordPhoto listRecordPhotos(
		@PathVariable UUID tripId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}
}
