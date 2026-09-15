package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 쓰레드 작성 요청.
 *
 * <p>{@code content}는 1~500자이며, {@code mediaFileIds}는 선택 항목으로 최대 4개까지 허용한다.
 * 미디어는 요청자가 소유한 활성 {@code media.media_files}만 연결할 수 있고, 목록 순서가 노출 순서가 된다.
 *
 * @param content 본문 (1~500자)
 * @param mediaFileIds 첨부할 미디어 파일 식별자 목록. 없으면 null 또는 빈 목록
 */
public record CreateCommunityThreadRequest(
	@NotBlank
	@Size(min = 1, max = 500)
	String content,
	@Size(max = 4)
	List<UUID> mediaFileIds
) {
}
