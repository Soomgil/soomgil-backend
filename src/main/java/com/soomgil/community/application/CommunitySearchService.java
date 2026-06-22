package com.soomgil.community.application;

import com.soomgil.community.api.dto.CommunityPostSummary;
import com.soomgil.community.application.service.CommunityPostAssembler;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 검색용 커뮤니티 게시글 검색 서비스.
 *
 * <p>공개 게시글 중 키워드가 매칭되는 항목을 좋아요 순으로 반환한다.
 */
@Service
public class CommunitySearchService {

	private final CommunityPostMapper postMapper;
	private final CommunityPostAssembler assembler;

	public CommunitySearchService(CommunityPostMapper postMapper, CommunityPostAssembler assembler) {
		this.postMapper = postMapper;
		this.assembler = assembler;
	}

	@Transactional(readOnly = true)
	public List<CommunityPostSummary> searchPublicPostsByLikes(String q, int size, UUID viewerUserId) {
		return postMapper.searchPublicFeedByLikes(q, 0, size).stream()
			.map(record -> assembler.toSummary(record, viewerUserId))
			.toList();
	}
}
