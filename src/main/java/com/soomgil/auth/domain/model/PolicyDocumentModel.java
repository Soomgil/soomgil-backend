package com.soomgil.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 약관 문서.
 *
 * @param id 문서 식별자
 * @param policyCode 약관 코드 (TERMS_OF_SERVICE, PRIVACY_POLICY 등)
 * @param version 버전
 * @param languageCode 언어 코드
 * @param title 제목
 * @param contentUrl 본문 URL (nullable)
 * @param contentHash 본문 hash (nullable)
 * @param isRequired 필수 약관 여부
 * @param publishedAt 공개 시각
 */
public record PolicyDocumentModel(
	UUID id,
	String policyCode,
	String version,
	String languageCode,
	String title,
	String contentUrl,
	String contentHash,
	boolean isRequired,
	Instant publishedAt
) {
}
