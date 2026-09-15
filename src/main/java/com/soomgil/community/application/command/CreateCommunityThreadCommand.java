package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityThread;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 쓰레드 작성 command.
 *
 * <p>Controller는 인증된 사용자 ID를 {@code actorUserId}에 담아 전달한다.
 * handler는 쓰레드와 첨부 미디어 연결을 같은 transaction 안에서 저장한다.
 *
 * @param actorUserId 작성자
 * @param content 본문
 * @param mediaFileIds 첨부 미디어 식별자 목록. 노출 순서를 그대로 사용한다
 */
public record CreateCommunityThreadCommand(
	UUID actorUserId,
	String content,
	List<UUID> mediaFileIds
) implements Command<CommunityThread> {
}
