package com.soomgil.preference.infrastructure.websocket;

import com.soomgil.preference.infrastructure.persistence.mapper.TripPreferencePlaceMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 최종 장소 반응 변경 후 해당 사용자의 활성 여행방 지도에 갱신 신호를 보낸다. */
@Component
public class PreferenceReactionRealtimePublisher {
    private final TripPreferencePlaceMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    public PreferenceReactionRealtimePublisher(
        TripPreferencePlaceMapper mapper, SimpMessagingTemplate messagingTemplate
    ) {
        this.mapper = mapper;
        this.messagingTemplate = messagingTemplate;
    }

    /** 반응과 프로필 공개 권한은 이벤트에 포함하지 않고, 구독자가 인증된 조회 API로 다시 읽는다. */
    public void publish(UUID userId) {
        Runnable send = () -> mapper.findActiveTripIdsByUser(userId.toString()).forEach(tripId ->
            messagingTemplate.convertAndSend(
                "/topic/trips/" + tripId + "/preferences",
                Map.of("eventType", "preference.reaction.updated", "tripId", tripId.toString())
            )
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { send.run(); }
            });
        } else {
            send.run();
        }
    }
}
