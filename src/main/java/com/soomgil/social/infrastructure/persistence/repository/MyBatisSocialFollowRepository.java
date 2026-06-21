package com.soomgil.social.infrastructure.persistence.repository;

import com.soomgil.social.application.port.SocialFollowRepository;
import com.soomgil.social.domain.model.SocialFollowRecord;
import com.soomgil.social.domain.model.SocialFollowRequestRecord;
import com.soomgil.social.infrastructure.persistence.mapper.SocialFollowMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** MyBatis 기반 follow 관계 repository. */
@Repository
public class MyBatisSocialFollowRepository implements SocialFollowRepository {

	private final SocialFollowMapper mapper;

	public MyBatisSocialFollowRepository(SocialFollowMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public String findProfileVisibility(UUID userId) {
		return mapper.findProfileVisibility(userId);
	}

	@Override
	public SocialFollowRecord upsert(UUID followerId, UUID followingId, String status, Instant now) {
		mapper.upsert(followerId, followingId, status, now);
		return mapper.find(followerId, followingId);
	}

	@Override
	public SocialFollowRecord find(UUID followerId, UUID followingId) {
		return mapper.find(followerId, followingId);
	}

	@Override
	public boolean activatePending(UUID followerId, UUID followingId, Instant now) {
		return mapper.activatePending(followerId, followingId, now) == 1;
	}

	@Override
	public boolean delete(UUID followerId, UUID followingId, String requiredStatus, Instant now) {
		return mapper.delete(followerId, followingId, requiredStatus, now) == 1;
	}

	@Override
	public List<SocialFollowRequestRecord> findPending(UUID followingId, int offset, int size) {
		return mapper.findPending(followingId, offset, size);
	}

	@Override
	public long countPending(UUID followingId) {
		return mapper.countPending(followingId);
	}
}
