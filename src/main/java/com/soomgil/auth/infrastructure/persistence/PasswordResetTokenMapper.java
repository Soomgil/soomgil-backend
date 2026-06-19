package com.soomgil.auth.infrastructure.persistence;

import com.soomgil.auth.domain.model.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * auth.user_password_reset_tokens 테이블에 대한 MyBatis mapper.
 */
@Mapper
public interface PasswordResetTokenMapper {

	@Insert("""
		INSERT INTO auth.user_password_reset_tokens (user_id, token_hash, expires_at)
		VALUES (#{userId}, #{tokenHash}, #{expiresAt})
		""")
	void insert(
		@Param("userId") UUID userId,
		@Param("tokenHash") String tokenHash,
		@Param("expiresAt") Instant expiresAt
	);

	@Select("""
		SELECT id, user_id, token_hash, expires_at, used_at
		FROM auth.user_password_reset_tokens
		WHERE token_hash = #{tokenHash}
		""")
	Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

	@Update("UPDATE auth.user_password_reset_tokens SET used_at = #{usedAt} WHERE id = #{id}")
	void markUsed(@Param("id") UUID id, @Param("usedAt") Instant usedAt);
}
