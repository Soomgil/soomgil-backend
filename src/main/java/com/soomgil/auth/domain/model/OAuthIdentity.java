package com.soomgil.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * OAuth 제공자 계정 연결.
 *
 * @param id 식별자
 * @param userId 숨길 사용자 식별자
 * @param providerId auth_providers 식별자
 * @param providerSubject 제공자 내 사용자 식별자
 * @param providerEmail 제공자에 등록된 이메일 (nullable)
 * @param providerDisplayName 제공자에 등록된 이름 (nullable)
 * @param linkedAt 연결 시각
 * @param lastLoginAt 마지막 로그인 시각 (nullable)
 */
public record OAuthIdentity(
	UUID id,
	UUID userId,
	short providerId,
	String providerSubject,
	String providerEmail,
	String providerDisplayName,
	Instant linkedAt,
	Instant lastLoginAt
) {
}
