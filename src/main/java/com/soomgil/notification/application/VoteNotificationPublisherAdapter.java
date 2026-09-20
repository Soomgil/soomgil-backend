package com.soomgil.notification.application;

import com.soomgil.notification.infrastructure.persistence.NotificationMapper;
import com.soomgil.voting.application.port.VoteNotificationPublisher;
import com.soomgil.notification.application.port.NotificationRealtimePublisher;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 투표 참여자 중 현재도 활성 멤버인 사용자에게만 시작·결과 알림을 저장한다. */
@Component
public class VoteNotificationPublisherAdapter implements VoteNotificationPublisher {
    private final NotificationMapper mapper;
    private final NotificationRealtimePublisher realtimePublisher;
    public VoteNotificationPublisherAdapter(NotificationMapper mapper, NotificationRealtimePublisher realtimePublisher) {
        this.mapper = mapper;
        this.realtimePublisher = realtimePublisher;
    }

    /** 투표 변경과 알림은 함께 커밋되거나 함께 취소된다. 외부 발송은 하지 않는다. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(UUID tripId, UUID sessionId, boolean completed, Instant createdAt) {
        mapper.insertVoteNotifications(tripId, sessionId, completed ? "VOTE_COMPLETED" : "VOTE_STARTED",
            completed ? "투표 결과가 도착했어요" : "여행 투표가 시작됐어요", createdAt);
        mapper.findVoteNotificationRecipientUserIds(tripId, sessionId).forEach(realtimePublisher::publishChanged);
    }
}
