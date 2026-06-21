package com.soomgil.community.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 모더레이션 조치 이력 페이지 응답.
 *
 * <p>모더레이션 대시보드에서 조치 audit 로그를 조회할 때 사용한다.
 *
 * @param items 조치 이력 목록
 * @param page 페이지 메타데이터
 */
public record PagedModerationAction(
	@Valid
	List<ModerationAction> items,
	@Valid
	PageMeta page
) {
}
