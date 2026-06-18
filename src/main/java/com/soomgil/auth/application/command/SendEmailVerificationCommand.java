package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;

/**
 * 이메일 인증 메일 발송 요청.
 *
 * @param email 인증할 이메일 주소
 */
public record SendEmailVerificationCommand(String email) implements Command<NoResult> {
}
