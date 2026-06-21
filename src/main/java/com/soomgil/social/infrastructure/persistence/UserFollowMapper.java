package com.soomgil.social.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserFollowMapper {

	@Select("""
		SELECT follower_user_id, following_user_id, status, created_at, updated_at
		FROM social.user_follows
		WHERE follower_user_id = #{followerUserId} AND following_user_id = #{followingUserId}
		""")
	Optional<UserFollowRecord> find(
		@Param("followerUserId") UUID followerUserId,
		@Param("followingUserId") UUID followingUserId
	);

	@Insert("""
		INSERT INTO social.user_follows (follower_user_id, following_user_id, status, created_at, updated_at)
		VALUES (#{followerUserId}, #{followingUserId}, #{status}, #{createdAt}, #{updatedAt})
		""")
	void insert(
		@Param("followerUserId") UUID followerUserId,
		@Param("followingUserId") UUID followingUserId,
		@Param("status") String status,
		@Param("createdAt") Instant createdAt,
		@Param("updatedAt") Instant updatedAt
	);

	@Update("""
		UPDATE social.user_follows
		SET status = #{status}, updated_at = #{updatedAt}
		WHERE follower_user_id = #{followerUserId} AND following_user_id = #{followingUserId}
		""")
	void updateStatus(
		@Param("followerUserId") UUID followerUserId,
		@Param("followingUserId") UUID followingUserId,
		@Param("status") String status,
		@Param("updatedAt") Instant updatedAt
	);

	@Select("""
		SELECT count(*)
		FROM social.user_follows
		WHERE following_user_id = #{userId} AND status = 'ACTIVE'
		""")
	int countFollowers(@Param("userId") UUID userId);

	@Select("""
		SELECT count(*)
		FROM social.user_follows
		WHERE follower_user_id = #{userId} AND status = 'ACTIVE'
		""")
	int countFollowing(@Param("userId") UUID userId);

	@Select("""
		SELECT p.user_id AS id, p.display_name AS displayName, p.profile_image_url AS profileImageUrl
		FROM social.user_follows f
		JOIN auth.user_profiles p ON f.follower_user_id = p.user_id
		WHERE f.following_user_id = #{userId} AND f.status = 'ACTIVE'
		""")
	List<UserSummaryRecord> findFollowers(@Param("userId") UUID userId);

	@Select("""
		SELECT p.user_id AS id, p.display_name AS displayName, p.profile_image_url AS profileImageUrl
		FROM social.user_follows f
		JOIN auth.user_profiles p ON f.following_user_id = p.user_id
		WHERE f.follower_user_id = #{userId} AND f.status = 'ACTIVE'
		""")
	List<UserSummaryRecord> findFollowing(@Param("userId") UUID userId);
}
