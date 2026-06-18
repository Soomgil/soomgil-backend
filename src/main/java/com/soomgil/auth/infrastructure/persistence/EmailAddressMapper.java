package com.soomgil.auth.infrastructure.persistence;

import com.soomgil.auth.domain.model.EmailAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * auth.user_email_addresses 테이블에 대한 MyBatis mapper.
 *
 * <p>회원가입 시 primary email을 생성하고, 로그인 시 normalized email로 조회하며, 중복을 확인한다.
 */
@Mapper
public interface EmailAddressMapper {

	@Insert("""
		INSERT INTO auth.user_email_addresses (user_id, email, normalized_email, is_primary, verified_at)
		VALUES (#{userId}, #{email}, #{normalizedEmail}, true, #{verifiedAt})
		""")
	void insertPrimary(
		@Param("userId") UUID userId,
		@Param("email") String email,
		@Param("normalizedEmail") String normalizedEmail,
		@Param("verifiedAt") Instant verifiedAt
	);

	@Select("""
		SELECT e.id, e.user_id, e.email, e.normalized_email, e.is_primary, e.verified_at
		FROM auth.user_email_addresses e
		WHERE e.normalized_email = #{normalizedEmail} AND e.removed_at IS NULL
		""")
	Optional<EmailAddress> findActiveByNormalizedEmail(@Param("normalizedEmail") String normalizedEmail);

	@Select("SELECT COUNT(*) > 0 FROM auth.user_email_addresses WHERE normalized_email = #{normalizedEmail} AND removed_at IS NULL")
	boolean existsActiveByNormalizedEmail(@Param("normalizedEmail") String normalizedEmail);

	@Select("""
		SELECT e.id, e.user_id, e.email, e.normalized_email, e.is_primary, e.verified_at
		FROM auth.user_email_addresses e
		WHERE e.user_id = #{userId} AND e.is_primary = true AND e.removed_at IS NULL
		""")
	Optional<EmailAddress> findPrimaryByUserId(@Param("userId") UUID userId);

	@Update("UPDATE auth.user_email_addresses SET verified_at = #{verifiedAt}, updated_at = now() WHERE id = #{id}")
	void updateVerifiedAt(@Param("id") UUID id, @Param("verifiedAt") Instant verifiedAt);

	@Update("UPDATE auth.user_email_addresses SET verification_last_sent_at = #{sentAt}, updated_at = now() WHERE id = #{id}")
	void updateVerificationLastSentAt(@Param("id") UUID id, @Param("sentAt") Instant sentAt);

	@Select("""
		SELECT e.id, e.user_id, e.email, e.normalized_email, e.is_primary, e.verified_at
		FROM auth.user_email_addresses e
		WHERE e.id = #{id}
		""")
	Optional<EmailAddress> findById(@Param("id") UUID id);
}
