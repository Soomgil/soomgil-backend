package com.soomgil.community.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 커뮤니티 게시글 요약 목록 페이지 응답.
 *
 * @param items 게시글 요약 목록
 * @param page 페이지 메타데이터
 */
public record PagedCommunityPostSummary(
	@Valid
	List<CommunityPostSummary> items,
	@Valid
	PageMeta page
) {
}
