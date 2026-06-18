package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import java.util.UUID;

/**
 * 특정 세션 폐기 요청.
 *
 * @param sessionId 세션 식별자
 * @param userId 요청자 식별자 (본인 세션만 폐기 가능)
 */
public record RevokeSessionCommand(UUID sessionId, UUID userId) implements Command<NoResult> {
}
