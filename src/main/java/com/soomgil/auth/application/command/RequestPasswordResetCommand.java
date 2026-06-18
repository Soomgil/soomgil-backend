package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;

/**
 * 비밀번호 재설정 요청 (이메일 발송).
 *
 * @param email 사용자 이메일
 */
public record RequestPasswordResetCommand(String email) implements Command<NoResult> {
}
