package com.soomgil.community.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 커뮤니티 쓰레드 목록 페이지 응답.
 *
 * @param items 쓰레드 목록. 최신순으로 정렬된다
 * @param page 0 기반 page 메타데이터
 */
public record PagedCommunityThread(
	@Valid
	List<CommunityThread> items,
	@Valid
	PageMeta page
) {
}
