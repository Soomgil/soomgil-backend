package com.soomgil.auth.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * auth.user_email_addresses 테이블에 대응하는 이메일 주소 model.
 *
 * <p>{@code normalizedEmail}는 lowercase canonical form으로 login 식별과 uniqueness에 사용한다.
 *
 * @param id 이메일 주소 식별자
 * @param userId 소속 사용자 식별자
 * @param email 원본 이메일
 * @param normalizedEmail 정규화된 이메일 (lowercase)
 * @param isPrimary 대표 이메일 여부
 * @param verifiedAt 이메일 인증 완료 시각
 */
public record EmailAddress(
	UUID id,
	UUID userId,
	String email,
	String normalizedEmail,
	boolean isPrimary,
	Instant verifiedAt
) {

	/**
	 * 원본 이메일에서 normalized email을 생성한다.
	 *
	 * @param email 원본 이메일 문자열
	 * @return lowercase trimmed email
	 */
	public static String normalize(String email) {
		Objects.requireNonNull(email, "email must not be null");
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
