package com.soomgil.social.infrastructure.persistence.mapper;

import com.soomgil.social.domain.model.SocialFollowRecord;
import com.soomgil.social.domain.model.SocialFollowRequestRecord;
import com.soomgil.social.domain.model.SocialFollowUserRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** follow CRUD와 PENDING 요청 page를 처리하는 SQL mapper. */
@Mapper
public interface SocialFollowMapper {

	String findProfileVisibility(UUID userId);

	void upsert(@Param("followerId") UUID followerId, @Param("followingId") UUID followingId,
		@Param("status") String status, @Param("now") Instant now);

	SocialFollowRecord find(@Param("followerId") UUID followerId, @Param("followingId") UUID followingId);

	int activatePending(@Param("followerId") UUID followerId, @Param("followingId") UUID followingId,
		@Param("now") Instant now);

	int delete(@Param("followerId") UUID followerId, @Param("followingId") UUID followingId,
		@Param("requiredStatus") String requiredStatus, @Param("now") Instant now);

	List<SocialFollowRequestRecord> findPending(@Param("followingId") UUID followingId,
		@Param("offset") int offset, @Param("size") int size);

	long countPending(UUID followingId);

	List<SocialFollowUserRecord> findFollowers(@Param("userId") UUID userId,
		@Param("offset") int offset, @Param("size") int size);

	long countFollowers(UUID userId);

	List<SocialFollowUserRecord> findFollowing(@Param("userId") UUID userId,
		@Param("offset") int offset, @Param("size") int size);

	long countFollowing(UUID userId);
}
