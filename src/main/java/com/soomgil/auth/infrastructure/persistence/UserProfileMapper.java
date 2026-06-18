package com.soomgil.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * auth.user_profiles 테이블에 대한 MyBatis mapper.
 *
 * <p>회원가입 시 display_name을 저장하고, 프로필 조회 시 읽어온다.
 */
@Mapper
public interface UserProfileMapper {

	@Insert("INSERT INTO auth.user_profiles (user_id, display_name) VALUES (#{userId}, #{displayName})")
	void insert(@Param("userId") UUID userId, @Param("displayName") String displayName);

	@Select("SELECT display_name FROM auth.user_profiles WHERE user_id = #{userId}")
	Optional<String> findDisplayName(@Param("userId") UUID userId);
}
