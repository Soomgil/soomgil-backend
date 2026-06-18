package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;
import java.util.UUID;

/**
 * 이메일 인증 확인 요청.
 *
 * @param token 인증 토큰 (raw)
 */
public record VerifyEmailCommand(String token) implements Command<UUID> {
}
