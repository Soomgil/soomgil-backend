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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class TripSubscriptionInterceptorTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	private final TripQueryRepository tripRepository = mock(TripQueryRepository.class);
	private final TripSubscriptionInterceptor interceptor = new TripSubscriptionInterceptor(
		new TripAccessGuard(tripRepository)
	);
	private final MessageChannel channel = mock(MessageChannel.class);

	@Test
	void allowsActiveMemberToSubscribeToTripTopic() {
		when(tripRepository.findTripAccess(TRIP_ID, USER_ID)).thenReturn(Optional.of(new TripAccessSnapshot(
			TRIP_ID,
			USER_ID,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			USER_ID
		)));
		Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/trips/" + TRIP_ID + "/itinerary", true);

		assertThat(interceptor.preSend(message, channel)).isSameAs(message);
	}

	@Test
	void rejectsConnectionWithoutAuthenticatedPrincipal() {
		Message<?> message = message(StompCommand.CONNECT, null, false);

		assertThatThrownBy(() -> interceptor.preSend(message, channel))
			.isInstanceOf(AccessDeniedException.class);
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
		StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
		accessor.setDestination(destination);
		if (authenticated) {
			accessor.setUser(() -> USER_ID.toString());
		}
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}
}
