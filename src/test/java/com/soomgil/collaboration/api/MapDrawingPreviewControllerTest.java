package com.soomgil.collaboration.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

class MapDrawingPreviewControllerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	private final TripAccessGuard tripAccessGuard = mock(TripAccessGuard.class);
	private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
	private final MapDrawingPreviewController controller = new MapDrawingPreviewController(
		tripAccessGuard,
		messagingTemplate
	);

	@Test
	void broadcastsPreviewToTripMapDrawingTopic() {
		Map<String, Object> payload = Map.of(
			"tripId", "malicious-trip",
			"clientId", "client-1",
			"previewId", "preview-1",
			"phase", "UPDATE"
		);

		controller.preview(TRIP_ID, payload, () -> USER_ID.toString());

		ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
		verify(tripAccessGuard).requireActiveMember(TRIP_ID, USER_ID);
		verify(messagingTemplate).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/map-drawings"),
			messageCaptor.capture()
		);
		org.assertj.core.api.Assertions.assertThat(messageCaptor.getValue())
			.containsEntry("tripId", TRIP_ID.toString())
			.containsEntry("clientId", "client-1")
			.containsEntry("previewId", "preview-1");
	}

	@Test
	void rejectsNonMemberPreview() {
		org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FORBIDDEN))
			.when(tripAccessGuard)
			.requireActiveMember(TRIP_ID, USER_ID);

		assertThatThrownBy(() -> controller.preview(TRIP_ID, Map.of(), () -> USER_ID.toString()))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void rejectsUnauthenticatedPreview() {
		assertThatThrownBy(() -> controller.preview(TRIP_ID, Map.of(), null))
			.isInstanceOf(AccessDeniedException.class);
	}
}
