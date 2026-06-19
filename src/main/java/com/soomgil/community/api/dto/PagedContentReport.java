package com.soomgil.community.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 콘텐츠 신고 목록 페이지 응답.
 *
 * <p>모더레이션 대시보드에서 신고 이력을 조회할 때 사용한다.
 *
 * @param items 신고 목록
 * @param page 페이지 메타데이터
 */
public record PagedContentReport(
	@Valid
	List<ContentReport> items,
	@Valid
	PageMeta page
) {
}
