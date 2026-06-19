package com.soomgil.collaboration.application.port;

import java.time.Instant;
import java.util.UUID;

/**
 * 협업 이벤트에 저장된 보상 command를 담당 도메인에 적용한다.
 */
public interface CollaborationCompensationExecutor {

	boolean supports(String commandPayload);

	void execute(UUID tripId, UUID actorUserId, String commandPayload, Instant executedAt);
}
