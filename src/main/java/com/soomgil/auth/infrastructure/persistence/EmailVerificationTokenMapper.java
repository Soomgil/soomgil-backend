package com.soomgil.auth.infrastructure.persistence;

import com.soomgil.auth.domain.model.EmailVerificationToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * auth.user_email_verification_tokens 테이블에 대한 MyBatis mapper.
 */
@Mapper
public interface EmailVerificationTokenMapper {

	@Insert("""
		INSERT INTO auth.user_email_verification_tokens (user_email_address_id, token_hash, expires_at)
		VALUES (#{emailAddressId}, #{tokenHash}, #{expiresAt})
		""")
	void insert(
		@Param("emailAddressId") UUID emailAddressId,
		@Param("tokenHash") String tokenHash,
		@Param("expiresAt") Instant expiresAt
	);

	@Select("""
		SELECT id, user_email_address_id, token_hash, expires_at, used_at
		FROM auth.user_email_verification_tokens
		WHERE token_hash = #{tokenHash}
		""")
	Optional<EmailVerificationToken> findByTokenHash(@Param("tokenHash") String tokenHash);

	@Update("UPDATE auth.user_email_verification_tokens SET used_at = #{usedAt} WHERE id = #{id}")
	void markUsed(@Param("id") UUID id, @Param("usedAt") Instant usedAt);
}
