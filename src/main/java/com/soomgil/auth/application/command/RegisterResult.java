package com.soomgil.auth.application.command;

import java.util.UUID;

/**
 * 이메일/비밀번호 회원가입 처리 결과.
 *
 * <p>가입 직후 계정은 {@code PENDING} 상태이며, 이메일 인증 완료 시
 * {@code ACTIVE}로 전환된다. 따라서 이 결과에는 access/refresh token이
 * 포함되지 않는다. 클라이언트는 안내 메시지와 함께 "이메일 인증 필요" UX를
 * 표시해야 한다.
 *
 * @param userId 생성된 사용자 id
 * @param email 가입한 이메일 (인증 메일이 발송된 주소)
 */
public record RegisterResult(UUID userId, String email) {
}
