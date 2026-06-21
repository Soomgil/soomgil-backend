package com.soomgil.community.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 게시글 snapshot에서 생성된 새 여행방의 provenance를 저장한다. */
@Mapper
public interface PostRetripMapper {

	@Insert("""
		INSERT INTO community.post_retrips (id, post_id, user_id, new_trip_id, snapshot_version, created_at)
		VALUES (#{id}, #{postId}, #{userId}, #{newTripId}, #{snapshotVersion}, #{createdAt})
		""")
	void insert(
		@Param("id") UUID id,
		@Param("postId") UUID postId,
		@Param("userId") UUID userId,
		@Param("newTripId") UUID newTripId,
		@Param("snapshotVersion") int snapshotVersion,
		@Param("createdAt") Instant createdAt
	);

	@Select("SELECT COUNT(*) FROM community.post_retrips WHERE post_id = #{postId}")
	int countByPostId(@Param("postId") UUID postId);
}
