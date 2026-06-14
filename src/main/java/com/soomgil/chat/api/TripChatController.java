package com.soomgil.chat.api;

import com.soomgil.chat.api.dto.CreateTripChatMessageRequest;
import com.soomgil.chat.api.dto.PagedTripChatMessage;
import com.soomgil.chat.api.dto.TripChatMessage;
import com.soomgil.common.api.ApiControllerSupport;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/chat/messages")
public class TripChatController extends ApiControllerSupport {

	@GetMapping
	public PagedTripChatMessage listMessages(
		@PathVariable UUID tripId,
		@RequestParam(defaultValue = "0") int offset,
		@RequestParam(defaultValue = "50") int limit,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TripChatMessage createMessage(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateTripChatMessageRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/{messageId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMessage(@PathVariable UUID tripId, @PathVariable UUID messageId) {
		notImplemented();
	}
}
