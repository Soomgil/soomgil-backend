package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 쓰레드 수정 요청. 작성자만 호출할 수 있다.
 *
 * <p>{@code mediaFileIds}를 생략(null)하면 기존 첨부 이미지를 그대로 유지하고,
 * 빈 목록을 보내면 모든 첨부 이미지를 제거한다. 값을 보내면 전달한 목록으로 전체 교체한다.
 *
 * @param content 새 본문 (1~500자)
 * @param mediaFileIds 교체할 미디어 파일 식별자 목록. null이면 기존 유지
 */
public record UpdateCommunityThreadRequest(
	@NotBlank
	@Size(min = 1, max = 500)
	String content,
	@Size(max = 4)
	List<UUID> mediaFileIds
) {
}
