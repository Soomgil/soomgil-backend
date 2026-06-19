package com.soomgil.community.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 커뮤니티 댓글 목록 페이지 응답.
 *
 * @param items 댓글 목록
 * @param page 페이지 메타데이터
 */
public record PagedCommunityComment(
	@Valid
	List<CommunityComment> items,
	@Valid
	PageMeta page
) {
}
