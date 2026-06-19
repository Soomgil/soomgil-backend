package com.soomgil.auth.infrastructure.persistence;

import com.soomgil.auth.api.dto.UserSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * auth.user_sessions 테이블에 대한 MyBatis mapper.
 *
 * <p>refresh token rotation과 재사용 감지를 지원한다.
 * token hash로 session을 조회하고, rotation 시 기존 session을 revoke한다.
 * 세션 목록 조회와 개별/전체 폐기도 지원한다.
 */
@Mapper
public interface UserSessionMapper {

	@Insert("""
		INSERT INTO auth.user_sessions (id, user_id, refresh_token_hash, refresh_token_family_id, expires_at)
		VALUES (#{id}, #{userId}, #{refreshTokenHash}, #{refreshTokenFamilyId}, #{expiresAt})
		""")
	void insert(
		@Param("id") UUID id,
		@Param("userId") UUID userId,
		@Param("refreshTokenHash") String refreshTokenHash,
		@Param("refreshTokenFamilyId") UUID refreshTokenFamilyId,
		@Param("expiresAt") Instant expiresAt
	);

	@Select("""
		SELECT id, user_id, refresh_token_hash, refresh_token_family_id, expires_at, revoked_at, created_at
		FROM auth.user_sessions
		WHERE refresh_token_hash = #{refreshTokenHash}
		""")
	Optional<com.soomgil.auth.domain.model.UserSession> findByRefreshTokenHash(
		@Param("refreshTokenHash") String refreshTokenHash
	);

	@Update("UPDATE auth.user_sessions SET revoked_at = #{revokedAt}, revocation_reason = #{reason} WHERE id = #{id}")
	void revoke(@Param("id") UUID id, @Param("revokedAt") Instant revokedAt, @Param("reason") String reason);

	@Update("UPDATE auth.user_sessions SET revoked_at = #{revokedAt}, revocation_reason = #{reason} WHERE refresh_token_family_id = #{familyId} AND revoked_at IS NULL")
	void revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt, @Param("reason") String reason);

	// ---- 세션 목록 / 개별 폐기 / 전체 폐기 ----

	@Select("""
		SELECT id, refresh_token_family_id, refresh_token_version,
		       device_name, device_os, last_used_at, last_refreshed_at,
		       expires_at, revoked_at, revocation_reason
		FROM auth.user_sessions
		WHERE user_id = #{userId}
		ORDER BY created_at DESC
		LIMIT #{limit} OFFSET #{offset}
		""")
	List<UserSession> findByUserId(
		@Param("userId") UUID userId,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Select("SELECT COUNT(*) FROM auth.user_sessions WHERE user_id = #{userId}")
	long countByUserId(@Param("userId") UUID userId);

	@Select("""
		SELECT id, user_id, refresh_token_hash, refresh_token_family_id, expires_at, revoked_at, created_at
		FROM auth.user_sessions
		WHERE id = #{id} AND user_id = #{userId}
		""")
	Optional<com.soomgil.auth.domain.model.UserSession> findByIdAndUserId(
		@Param("id") UUID id,
		@Param("userId") UUID userId
	);

	@Update("""
		UPDATE auth.user_sessions
		SET revoked_at = #{revokedAt}, revocation_reason = #{reason}
		WHERE user_id = #{userId} AND revoked_at IS NULL
		""")
	void revokeAllForUser(
		@Param("userId") UUID userId,
		@Param("revokedAt") Instant revokedAt,
		@Param("reason") String reason
	);
}
