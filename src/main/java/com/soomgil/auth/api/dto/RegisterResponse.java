package com.soomgil.auth.api.dto;

import java.util.UUID;

/**
 * 회원가입 응답.
 *
 * <p>가입 직후 계정은 {@code PENDING} 상태이며, 이메일 인증이 완료되어야
 * {@code ACTIVE}로 전환되어 로그인할 수 있다. 따라서 이 응답에는 access/refresh
 * token이 포함되지 않는다. 클라이언트는 {@code message}를 사용자에게 안내하고
 * 인증 메일 확인을 유도해야 한다.
 *
 * @param userId 생성된 사용자 id
 * @param email 가입한 이메일 (인증 메일이 발송된 주소)
 * @param message 클라이언트가 사용자에게 표시할 안내 메시지
 */
public record RegisterResponse(UUID userId, String email, String message) {
}
