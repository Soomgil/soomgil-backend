package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.community.api.dto.CommunityThread;
import com.soomgil.community.application.query.GetCommunityThreadQuery;
import com.soomgil.community.application.service.CommunityThreadAssembler;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.global.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link GetCommunityThreadQuery}를 처리해 쓰레드 단건을 조회한다.
 *
 * <p>공개 조회이므로 인증을 요구하지 않는다. 삭제되거나 숨김 처리된 쓰레드는 존재 여부를 은닉하기 위해
 * {@code THREAD_NOT_FOUND}로 응답한다. 작성자 본인이라도 삭제한 쓰레드는 다시 조회되지 않는다.
 */
@Component
@Transactional(readOnly = true)
public class GetCommunityThreadQueryHandler
	implements QueryHandler<GetCommunityThreadQuery, CommunityThread> {

	private final CommunityThreadMapper threadMapper;
	private final CommunityThreadAssembler assembler;

	public GetCommunityThreadQueryHandler(
		CommunityThreadMapper threadMapper,
		CommunityThreadAssembler assembler
	) {
		this.threadMapper = threadMapper;
		this.assembler = assembler;
	}

	@Override
	public CommunityThread handle(GetCommunityThreadQuery query) {
		CommunityThreadRecord thread = threadMapper.findById(query.threadId())
			.filter(CommunityThreadRecord::isPubliclyReadable)
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_NOT_FOUND));
		return assembler.toThread(thread, query.viewerUserId());
	}
}
