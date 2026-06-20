package com.soomgil.social.application.port;

import com.soomgil.social.domain.model.SocialFollowRecord;
import com.soomgil.social.domain.model.SocialFollowRequestRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** follow 관계 저장과 대기 요청 조회를 담당하는 persistence 계약. */
public interface SocialFollowRepository {

	String findProfileVisibility(UUID userId);

	SocialFollowRecord upsert(UUID followerId, UUID followingId, String status, Instant now);

	SocialFollowRecord find(UUID followerId, UUID followingId);

	boolean activatePending(UUID followerId, UUID followingId, Instant now);

	boolean delete(UUID followerId, UUID followingId, String requiredStatus, Instant now);

	List<SocialFollowRequestRecord> findPending(UUID followingId, int offset, int size);

	long countPending(UUID followingId);
}
