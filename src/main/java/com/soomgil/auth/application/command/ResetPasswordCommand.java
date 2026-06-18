package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;

/**
 * 비밀번호 재설정 확정.
 *
 * @param token 재설정 토큰 (raw)
 * @param newPassword 새 비밀번호
 */
public record ResetPasswordCommand(String token, String newPassword) implements Command<NoResult> {
}
