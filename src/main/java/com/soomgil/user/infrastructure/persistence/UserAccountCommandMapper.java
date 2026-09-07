package com.soomgil.user.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;

/**
 * {@code auth.users} 테이블의 계정 상태 lifecycle을 갱신하는 MyBatis mapper.
 *
 * <p>user 도메인의 즉시 계정 탈퇴({@code DELETE /me}) 흐름에서만 사용한다.
 * 로그인/상태 조회는 {@code auth.UserMapper}에 그대로 둔다.
 */
@Mapper
public interface UserAccountCommandMapper {

	/**
	 * 계정을 즉시 {@code DELETED} 상태로 전환한다.
	 *
	 * <p>이 메서드가 1을 반환한 경우에만 같은 transaction 안에서 개인정보 익명화를 진행한다.
	 *
	 * @param userId 사용자 식별자
	 * @param deletedAt 탈퇴 처리 시각
	 * @return 갱신된 행 수. 정상 흐름에서 1
	 */
	@Update("""
		UPDATE auth.users
		SET status = 'DELETED',
		    status_reason = 'User deleted account',
		    status_changed_at = now(),
		    deletion_requested_at = #{deletedAt},
		    deletion_scheduled_at = NULL,
		    deleted_at = #{deletedAt},
		    updated_at = now()
		WHERE id = #{userId} AND status = 'ACTIVE'
		""")
	int markDeleted(
		@Param("userId") UUID userId,
		@Param("deletedAt") OffsetDateTime deletedAt
	);

	@Update("""
		UPDATE auth.user_sessions
		SET revoked_at = COALESCE(revoked_at, #{deletedAt}),
		    revocation_reason = 'ACCOUNT_DELETED',
		    device_name = NULL,
		    device_os = NULL,
		    ip_address_hash = NULL,
		    user_agent_hash = NULL
		WHERE user_id = #{userId}
		""")
	void revokeSessions(@Param("userId") UUID userId, @Param("deletedAt") OffsetDateTime deletedAt);

	@Delete("""
		DELETE FROM auth.user_email_verification_tokens
		WHERE user_email_address_id IN (
			SELECT id FROM auth.user_email_addresses WHERE user_id = #{userId}
		)
		""")
	void deleteEmailVerificationTokens(@Param("userId") UUID userId);

	@Delete("DELETE FROM auth.user_password_reset_tokens WHERE user_id = #{userId}")
	void deletePasswordResetTokens(@Param("userId") UUID userId);

	@Delete("DELETE FROM auth.user_password_credentials WHERE user_id = #{userId}")
	void deletePasswordCredential(@Param("userId") UUID userId);

	@Delete("DELETE FROM auth.user_auth_identities WHERE user_id = #{userId}")
	void deleteOAuthIdentities(@Param("userId") UUID userId);

	@Update("""
		UPDATE auth.user_email_addresses
		SET email = 'deleted+' || id::text || '@deleted.invalid',
		    normalized_email = 'deleted+' || id::text || '@deleted.invalid',
		    is_primary = false,
		    verified_at = NULL,
		    verification_last_sent_at = NULL,
		    removed_at = #{deletedAt},
		    removed_reason = 'ACCOUNT_DELETED',
		    updated_at = now()
		WHERE user_id = #{userId}
		""")
	void anonymizeEmailAddresses(@Param("userId") UUID userId, @Param("deletedAt") OffsetDateTime deletedAt);

	@Update("""
		UPDATE auth.user_profiles
		SET display_name = '탈퇴한 사용자',
		    profile_image_url = NULL,
		    profile_media_file_id = NULL,
		    bio = NULL,
		    profile_visibility = 'PRIVATE',
		    updated_at = now()
		WHERE user_id = #{userId}
		""")
	void anonymizeProfile(@Param("userId") UUID userId);

	@Update("""
		UPDATE auth.user_settings
		SET marketing_email_opt_in = false,
		    marketing_email_opted_out_at = #{deletedAt},
		    trip_invite_email_opt_in = false,
		    updated_at = now()
		WHERE user_id = #{userId}
		""")
	void disableEmailSettings(@Param("userId") UUID userId, @Param("deletedAt") OffsetDateTime deletedAt);

	@Update("""
		UPDATE auth.user_security_events
		SET normalized_identifier = NULL,
		    ip_address_hash = NULL,
		    user_agent_hash = NULL,
		    metadata = NULL
		WHERE user_id = #{userId} OR actor_user_id = #{userId}
		""")
	void anonymizeSecurityEvents(@Param("userId") UUID userId);
}
