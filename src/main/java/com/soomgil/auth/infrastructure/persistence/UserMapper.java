package com.soomgil.auth.infrastructure.persistence;

import com.soomgil.auth.domain.model.AuthUser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * auth.users 테이블에 대한 MyBatis mapper.
 *
 * <p>회원가입 시 row를 생성하고, 로그인 시 lastLoginAt을 갱신하며, id로 계정을 조회한다.
 */
@Mapper
public interface UserMapper {

	@Insert("INSERT INTO auth.users (id, status) VALUES (#{id}, #{status})")
	void insert(@Param("id") UUID id, @Param("status") String status);

	@Select("SELECT id, status, last_login_at, created_at FROM auth.users WHERE id = #{id}")
	Optional<AuthUser> findById(@Param("id") UUID id);

	@Update("UPDATE auth.users SET last_login_at = #{lastLoginAt}, updated_at = now() WHERE id = #{id}")
	void updateLastLoginAt(@Param("id") UUID id, @Param("lastLoginAt") Instant lastLoginAt);

	@Update("UPDATE auth.users SET status = #{status}, status_changed_at = now(), updated_at = now() WHERE id = #{id}")
	void updateStatus(@Param("id") UUID id, @Param("status") String status);

	/**
	 * 사용자에게 부여된 역할 목록을 조회한다({@code auth.user_roles}).
	 *
	 * @param id 사용자 식별자
	 * @return 역할 문자열 목록 (예: {@code MODERATOR}, {@code ADMIN})
	 */
	@Select("SELECT role FROM auth.user_roles WHERE user_id = #{id}")
	List<String> findRolesByUserId(@Param("id") UUID id);
}
