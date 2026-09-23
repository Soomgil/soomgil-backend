package com.soomgil.preference.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.soomgil.preference.infrastructure.persistence.mapper.TripPreferencePlaceMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class PreferenceReactionRealtimePublisherTest {
    @Test
    void sendsOnlyAnInvalidationAfterCommitToEveryActiveTrip() {
        TripPreferencePlaceMapper mapper = mock(TripPreferencePlaceMapper.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        UUID userId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        when(mapper.findActiveTripIdsByUser(userId.toString())).thenReturn(List.of(tripId));
        PreferenceReactionRealtimePublisher publisher = new PreferenceReactionRealtimePublisher(mapper, messaging);

        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publish(userId);
            verifyNoInteractions(mapper, messaging);
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
            TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();
            verify(messaging).convertAndSend(eq("/topic/trips/" + tripId + "/preferences"),
                eq(Map.of("eventType", "preference.reaction.updated", "tripId", tripId.toString())));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
