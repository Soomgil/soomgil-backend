package com.soomgil.notification.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.auth.application.service.MailService;
import com.soomgil.notification.infrastructure.persistence.NotificationMapper;
import com.soomgil.notification.infrastructure.persistence.TripInviteEmailRecipientMapper;
import com.soomgil.notification.application.port.NotificationRealtimePublisher;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripInviteNotificationPublisherAdapterTest {

	private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
	private final TripInviteEmailRecipientMapper recipientMapper = mock(TripInviteEmailRecipientMapper.class);
	private final MailService mailService = mock(MailService.class);
	private final NotificationRealtimePublisher realtimePublisher = mock(NotificationRealtimePublisher.class);
	private final TripInviteNotificationPublisherAdapter publisher = new TripInviteNotificationPublisherAdapter(
		notificationMapper, new ObjectMapper(), recipientMapper, mailService, realtimePublisher
	);

	@Test
	void sendsEmailOnlyWhenRecipientHasOptedIn() {
		UUID recipientId = UUID.randomUUID();
		when(recipientMapper.findOptedInVerifiedEmail(recipientId)).thenReturn("traveler@example.com");

		publisher.publish(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), recipientId,
			"JOIN-ME", Instant.now());

		verify(mailService).sendTripInviteEmail("traveler@example.com", "JOIN-ME");
	}

	@Test
	void skipsEmailWhenRecipientHasOptedOut() {
		UUID recipientId = UUID.randomUUID();

		publisher.publish(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), recipientId,
			"JOIN-ME", Instant.now());

		verify(mailService, never()).sendTripInviteEmail("traveler@example.com", "JOIN-ME");
	}
}
