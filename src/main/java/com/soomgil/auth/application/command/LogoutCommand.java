package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import java.util.UUID;

/**
 * 로그아웃 요청.
 *
 * @param userId 사용자 식별자
 * @param refreshToken 사용한 refresh token (nullable — allDevices=true면 무시)
 * @param allDevices 모든 기기에서 로그아웃할지 여부
 */
public record LogoutCommand(UUID userId, String refreshToken, boolean allDevices) implements Command<NoResult> {
}
