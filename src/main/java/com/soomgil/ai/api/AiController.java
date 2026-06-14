package com.soomgil.ai.api;

import com.soomgil.ai.api.dto.AiChatSession;
import com.soomgil.ai.api.dto.AiMessageResponse;
import com.soomgil.ai.api.dto.AiToolCall;
import com.soomgil.ai.api.dto.CreateAiMessageRequest;
import com.soomgil.ai.api.dto.PagedAiChatMessage;
import com.soomgil.common.api.ApiControllerSupport;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/ai")
public class AiController extends ApiControllerSupport {

	@GetMapping("/session")
	public AiChatSession getSession(@PathVariable UUID tripId) {
		return notImplemented();
	}

	@GetMapping("/messages")
	public PagedAiChatMessage listMessages(
		@PathVariable UUID tripId,
		@RequestParam(defaultValue = "0") int offset,
		@RequestParam(defaultValue = "50") int limit,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@PostMapping({"/messages", "/chat"})
	public AiMessageResponse createMessage(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateAiMessageRequest request
	) {
		return notImplemented();
	}

	@PostMapping("/route-draft")
	public AiMessageResponse createRouteDraft(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateAiMessageRequest request
	) {
		return notImplemented();
	}

	@GetMapping("/tool-calls/{toolCallId}")
	public AiToolCall getToolCall(
		@PathVariable UUID tripId,
		@PathVariable UUID toolCallId
	) {
		return notImplemented();
	}
}
