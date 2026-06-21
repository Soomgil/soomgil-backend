package com.soomgil.auth.infrastructure.persistence;

import com.soomgil.auth.domain.model.PasswordCredential;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * auth.user_password_credentials 테이블에 대한 MyBatis mapper.
 *
 * <p>회원가입 시 bcrypt hash를 저장하고, 로그인 시 hash를 조회한다.
 * 로그인 실패 count를 증가시키거나 성공 시 reset한다.
 */
@Mapper
public interface PasswordCredentialMapper {

	@Insert("INSERT INTO auth.user_password_credentials (user_id, password_hash) VALUES (#{userId}, #{passwordHash})")
	void insert(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash);

	@Select("SELECT user_id, password_hash, failed_login_count, locked_until FROM auth.user_password_credentials WHERE user_id = #{userId}")
	Optional<PasswordCredential> findByUserId(@Param("userId") UUID userId);

	@Update("UPDATE auth.user_password_credentials SET failed_login_count = 0, updated_at = now() WHERE user_id = #{userId}")
	void resetFailedLoginCount(@Param("userId") UUID userId);

	@Update("UPDATE auth.user_password_credentials SET password_hash = #{passwordHash}, password_changed_at = now(), updated_at = now() WHERE user_id = #{userId}")
	void updatePasswordHash(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash);
}
