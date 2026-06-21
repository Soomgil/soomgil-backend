package com.soomgil.community.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 게시글에 첨부된 미디어 row.
 *
 * <p>한 게시글에 여러 미디어가 {@code sortOrder} 순서로 정렬된다.
 *
 * @param id 미디어 레코드 식별자
 * @param postId 게시글 식별자
 * @param mediaFileId 원본 MediaFile 식별자
 * @param sortOrder 정렬 순서 (게시글 내 고유)
 * @param caption 캡션 (nullable)
 * @param createdAt 생성 시각
 */
public record PostMediaRecord(
	UUID id,
	UUID postId,
	UUID mediaFileId,
	int sortOrder,
	String caption,
	Instant createdAt
) {
}
