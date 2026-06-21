package com.soomgil.ai.application;

import com.soomgil.geo.api.dto.Viewport;
import java.util.List;
import java.util.UUID;

public record AiGuideRequest(
	UUID tripId,
	UUID requesterUserId,
	String sessionSummary,
	List<AiGuideTurn> recentMessages,
	String question,
	Long baseVersion,
	Viewport viewport
) {
	public record AiGuideTurn(String role, String content) {
	}
}
