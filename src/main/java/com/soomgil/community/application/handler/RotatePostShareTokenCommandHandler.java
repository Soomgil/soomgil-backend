package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityPostShareTokenResponse;
import com.soomgil.community.application.command.RotatePostShareTokenCommand;
import com.soomgil.community.application.service.ShareTokenService;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.global.error.ErrorCode;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 공유 토큰을 (재)발급한다.
 *
 * <p>UNLISTED 게시글의 기존 토큰을 무효화하고 새 토큰을 발급한다. raw 토큰은 이 응답에서
 * 딱 한 번만 반환되고, DB에는 SHA-256 hash만 저장된다. 발행자 본인만 rotate할 수 있다.
 */
@Component
@Transactional
public class RotatePostShareTokenCommandHandler
	implements CommandHandler<RotatePostShareTokenCommand, CommunityPostShareTokenResponse> {

	private final CommunityPostMapper postMapper;
	private final ShareTokenService shareTokenService;

	public RotatePostShareTokenCommandHandler(
		CommunityPostMapper postMapper,
		ShareTokenService shareTokenService
	) {
		this.postMapper = postMapper;
		this.shareTokenService = shareTokenService;
	}

	@Override
	public CommunityPostShareTokenResponse handle(RotatePostShareTokenCommand command) {
		CommunityPostRecord post = postMapper.findById(command.postId())
			.filter(p -> !p.isDeleted())
			.orElseThrow(() -> new CommunityException(ErrorCode.POST_NOT_FOUND));

		if (!post.isPublishedBy(command.actorUserId())) {
			throw new CommunityException(ErrorCode.POST_AUTHOR_REQUIRED);
		}

		Instant now = Instant.now();
		ShareTokenService.IssuedShareToken issued = shareTokenService.issue();
		postMapper.updateShareToken(command.postId(), issued.hash(), now);

		URI shareUrl = URI.create(
			"https://soomgil.example.com/community/posts/" + command.postId() + "?share=" + issued.raw()
		);

		return new CommunityPostShareTokenResponse(
			command.postId(),
			issued.raw(),
			shareUrl,
			OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
		);
	}
}
