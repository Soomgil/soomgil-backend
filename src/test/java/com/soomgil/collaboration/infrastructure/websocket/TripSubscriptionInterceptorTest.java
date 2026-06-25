package com.soomgil.collaboration.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

class TripSubscriptionInterceptorTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	private final TripQueryRepository tripRepository = mock(TripQueryRepository.class);
	private final CollaborationWebSocketSessionRegistry sessionRegistry = new CollaborationWebSocketSessionRegistry();
	private final TripSubscriptionInterceptor interceptor = new TripSubscriptionInterceptor(
		new TripAccessGuard(tripRepository),
		sessionRegistry
	);
	private final MessageChannel channel = mock(MessageChannel.class);

	@Test
	void allowsActiveMemberToSubscribeToTripTopics() {
		when(tripRepository.findTripAccess(TRIP_ID, USER_ID)).thenReturn(Optional.of(new TripAccessSnapshot(
			TRIP_ID,
			USER_ID,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			USER_ID
		)));

		for (String topic : List.of(
			"collaboration",
			"itinerary",
			"map-drawings",
			"route-matching",
			"chat",
			"planning",
			"ai"
		)) {
			Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/trips/" + TRIP_ID + "/" + topic, true);

			assertThat(interceptor.preSend(message, channel)).isSameAs(message);
		}
	}

	@Test
	void rejectsConnectionWithoutAuthenticatedPrincipal() {
		Message<?> message = message(StompCommand.CONNECT, null, false);

		assertThatThrownBy(() -> interceptor.preSend(message, channel))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void registersAndRemovesAuthenticatedSession() {
		Message<?> connect = message(StompCommand.CONNECT, null, true, "session-1");
		Message<?> disconnect = message(StompCommand.DISCONNECT, null, false, "session-1");

		interceptor.preSend(connect, channel);
		assertThat(sessionRegistry.isOwnedBy("session-1", USER_ID)).isTrue();

		interceptor.preSend(disconnect, channel);
		assertThat(sessionRegistry.isOwnedBy("session-1", USER_ID)).isFalse();
	}

	@Test
	void authorizesSubscriptionFromRegisteredSessionWhenPrincipalIsMissing() {
		when(tripRepository.findTripAccess(TRIP_ID, USER_ID)).thenReturn(Optional.of(new TripAccessSnapshot(
			TRIP_ID,
			USER_ID,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			USER_ID
		)));
		sessionRegistry.register("session-1", USER_ID);
		Message<?> message = message(
			StompCommand.SUBSCRIBE,
			"/topic/trips/" + TRIP_ID + "/map-drawings",
			false,
			"session-1"
		);

		assertThat(interceptor.preSend(message, channel)).isSameAs(message);
	}

	@Test
	void removesSessionWhenTransportDisconnectEventArrives() {
		SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
		when(event.getSessionId()).thenReturn("session-1");
		sessionRegistry.register("session-1", USER_ID);

		sessionRegistry.handleDisconnect(event);

		assertThat(sessionRegistry.isOwnedBy("session-1", USER_ID)).isFalse();
	}

	@Test
	void rejectsNonMemberTripSubscription() {
		Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/trips/" + TRIP_ID + "/collaboration", true);

		assertThatThrownBy(() -> interceptor.preSend(message, channel))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void rejectsUnexpectedTopic() {
		Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/admin", true);

		assertThatThrownBy(() -> interceptor.preSend(message, channel))
			.isInstanceOf(AccessDeniedException.class);
	}

	private Message<?> message(StompCommand command, String destination, boolean authenticated) {
		return message(command, destination, authenticated, null);
	}

	private Message<?> message(StompCommand command, String destination, boolean authenticated, String sessionId) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
		accessor.setDestination(destination);
		accessor.setSessionId(sessionId);
		if (authenticated) {
			accessor.setUser(() -> USER_ID.toString());
		}
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}
}
