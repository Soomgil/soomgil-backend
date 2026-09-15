package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityThread;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 쓰레드 수정 command.
 *
 * <p>작성자만 수정할 수 있으며 작성자가 아니면 {@code THREAD_AUTHOR_REQUIRED}로 거부된다.
 * {@code mediaFileIds}가 null이면 기존 첨부를 유지하고, 값이 있으면 전체 교체한다.
 *
 * @param threadId 수정할 쓰레드 식별자
 * @param actorUserId 요청 사용자
 * @param content 새 본문
 * @param mediaFileIds 교체할 첨부 미디어 목록. null이면 기존 유지
 */
public record UpdateCommunityThreadCommand(
	UUID threadId,
	UUID actorUserId,
	String content,
	List<UUID> mediaFileIds
) implements Command<CommunityThread> {
}
