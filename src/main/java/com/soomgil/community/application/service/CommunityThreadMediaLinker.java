package com.soomgil.community.application.service;

import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.policy.CommunityThreadPolicy;
import com.soomgil.community.infrastructure.persistence.mapper.ThreadMediaMapper;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.media.application.MediaFileQueryService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 쓰레드 첨부 이미지 검증과 연결을 담당하는 내부 helper.
 *
 * <p>작성과 수정 handler가 같은 검증 순서를 공유하기 위해 분리했다. 검증 순서는
 * 개수 확인 → 본문 확인 → 소유권 확인이며, 소유권 확인을 통과하지 못한 요청은 어떤 row도 저장하지 않는다.
 */
public class CommunityThreadMediaLinker {

	private final ThreadMediaMapper threadMediaMapper;
	private final MediaFileQueryService mediaFileQueryService;

	public CommunityThreadMediaLinker(
		ThreadMediaMapper threadMediaMapper,
		MediaFileQueryService mediaFileQueryService
	) {
		this.threadMediaMapper = threadMediaMapper;
		this.mediaFileQueryService = mediaFileQueryService;
	}

	/**
	 * 첨부 이미지 개수가 정책 범위인지 검증한다.
	 *
	 * @param mediaFileIds 첨부 미디어 목록. null이면 첨부가 없는 것으로 본다
	 * @throws CommunityException 개수가 정책을 넘으면 {@code THREAD_MEDIA_LIMIT_EXCEEDED}
	 */
	public void validateMediaCount(List<UUID> mediaFileIds) {
		int count = mediaFileIds == null ? 0 : mediaFileIds.size();
		if (!CommunityThreadPolicy.isValidMediaCount(count)) {
			throw new CommunityException(ErrorCode.THREAD_MEDIA_LIMIT_EXCEEDED);
		}
	}

	/**
	 * 모든 첨부 미디어가 요청자 소유의 활성 미디어인지 검증한다.
	 *
	 * @param mediaFileIds 첨부 미디어 목록. null이면 검증하지 않는다
	 * @param ownerUserId 소유자로 기대하는 요청 사용자
	 * @throws CommunityException 소유자가 다르거나 미디어가 없으면 {@code MEDIA_LINK_FORBIDDEN}
	 */
	public void validateOwnership(List<UUID> mediaFileIds, UUID ownerUserId) {
		if (mediaFileIds == null) {
			return;
		}
		for (UUID mediaFileId : mediaFileIds) {
			if (!mediaFileQueryService.isOwnedActiveMedia(mediaFileId, ownerUserId)) {
				throw new CommunityException(ErrorCode.MEDIA_LINK_FORBIDDEN);
			}
		}
	}

	/**
	 * 첨부 미디어를 요청 순서대로 연결한다.
	 *
	 * @param threadId 대상 쓰레드 식별자
	 * @param mediaFileIds 첨부 미디어 목록. null 또는 빈 목록이면 아무 것도 하지 않는다
	 * @param now 연결 시각
	 */
	public void link(UUID threadId, List<UUID> mediaFileIds, Instant now) {
		if (mediaFileIds == null || mediaFileIds.isEmpty()) {
			return;
		}
		for (int index = 0; index < mediaFileIds.size(); index++) {
			threadMediaMapper.insert(threadId, mediaFileIds.get(index), index, now);
		}
	}

	/**
	 * 기존 연결을 모두 지우고 새 목록으로 교체한다.
	 *
	 * @param threadId 대상 쓰레드 식별자
	 * @param mediaFileIds 교체할 미디어 목록. 빈 목록이면 첨부를 모두 제거한다
	 * @param now 연결 시각
	 */
	public void replace(UUID threadId, List<UUID> mediaFileIds, Instant now) {
		threadMediaMapper.deleteByThreadId(threadId);
		link(threadId, mediaFileIds, now);
	}
}
