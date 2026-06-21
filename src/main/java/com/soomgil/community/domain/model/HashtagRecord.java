package com.soomgil.community.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 해시태그 마스터 row.
 *
 * <p>사용자 입력은 {@link #normalize(String)}으로 정규화한 뒤 {@code normalized_name}에 저장한다.
 * 동일한 normalized_name이면 같은 row를 재사용하고 {@code usage_count}를 증감한다.
 *
 * @param id 해시태그 식별자
 * @param name 표시용 이름 (예: "부산맛집")
 * @param normalizedName 정규화된 이름 (예: "부산맛집" → lowercased + trimmed)
 * @param usageCount 사용된 게시글 수
 * @param createdAt 생성 시각
 * @param updatedAt 마지막 갱신 시각
 */
public record HashtagRecord(
	UUID id,
	String name,
	String normalizedName,
	long usageCount,
	Instant createdAt,
	Instant updatedAt
) {

	/**
	 * 사용자 입력 해시태그를 정규화한다.
	 *
	 * <p>앞뒤 공백 제거, 연속 공백 단일화, 소문자 변환. {@code #} 접두어는 제거한다.
	 *
	 * @param raw 사용자 입력
	 * @return 정규화된 이름. {@code null}/공백이면 {@code null}
	 */
	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String stripped = raw.trim();
		if (stripped.startsWith("#")) {
			stripped = stripped.substring(1).trim();
		}
		if (stripped.isBlank()) {
			return null;
		}
		return stripped.replaceAll("\\s+", " ").toLowerCase();
	}
}
