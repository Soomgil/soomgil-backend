package com.soomgil.voting.application.port;

import java.time.Instant;
import java.util.UUID;

/** 투표 시작·결과 확정을 같은 트랜잭션의 참여자 인앱 알림으로 전달하는 포트. */
public interface VoteNotificationPublisher {
    /** completed가 true이면 결과 확정, false이면 시작 알림을 생성한다. 동일 세션·종류·수신자는 중복 저장하지 않는다. */
    void publish(UUID tripId, UUID sessionId, boolean completed, Instant createdAt);
}
