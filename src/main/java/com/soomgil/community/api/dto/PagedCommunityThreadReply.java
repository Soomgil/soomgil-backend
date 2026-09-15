package com.soomgil.community.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 커뮤니티 쓰레드 답글 목록 페이지 응답.
 *
 * <p>page 메타데이터는 root 답글 기준이며, 1단계 하위 답글은 각 root 답글의
 * {@code replies}에 포함되어 page 계산에 반영되지 않는다.
 *
 * @param items root 답글 목록
 * @param page 0 기반 page 메타데이터
 */
public record PagedCommunityThreadReply(
	@Valid
	List<CommunityThreadReply> items,
	@Valid
	PageMeta page
) {
}
