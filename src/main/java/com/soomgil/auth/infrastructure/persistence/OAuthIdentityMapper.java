package com.soomgil.auth.infrastructure.persistence;

import com.soomgil.auth.domain.model.OAuthIdentity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * auth.user_auth_identities 테이블에 대한 MyBatis mapper.
 */
@Mapper
public interface OAuthIdentityMapper {

	@Select("""
		SELECT id, user_id, provider_id, provider_subject, provider_email,
		       provider_display_name, linked_at, last_login_at
		FROM auth.user_auth_identities
		WHERE provider_id = #{providerId} AND provider_subject = #{providerSubject}
		""")
	Optional<OAuthIdentity> findByProviderAndSubject(
		@Param("providerId") short providerId,
		@Param("providerSubject") String providerSubject
	);

	@Insert("""
		INSERT INTO auth.user_auth_identities
			(user_id, provider_id, provider_subject, provider_email, provider_display_name)
		VALUES
			(#{userId}, #{providerId}, #{providerSubject}, #{providerEmail}, #{providerDisplayName})
		""")
	void insert(
		@Param("userId") UUID userId,
		@Param("providerId") short providerId,
		@Param("providerSubject") String providerSubject,
		@Param("providerEmail") String providerEmail,
		@Param("providerDisplayName") String providerDisplayName
	);

	@Update("UPDATE auth.user_auth_identities SET last_login_at = #{lastLoginAt}, updated_at = now() WHERE id = #{id}")
	void updateLastLoginAt(@Param("id") UUID id, @Param("lastLoginAt") Instant lastLoginAt);
}
