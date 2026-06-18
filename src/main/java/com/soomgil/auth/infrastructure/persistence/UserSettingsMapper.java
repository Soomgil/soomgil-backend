package com.soomgil.auth.infrastructure.persistence;

import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

/**
 * auth.user_settings 테이블에 대한 MyBatis mapper.
 *
 * <p>회원가입 시 기본 설정 row를 생성한다. 모든 값은 DB default를 따른다.
 */
@Mapper
public interface UserSettingsMapper {

	@Insert("INSERT INTO auth.user_settings (user_id) VALUES (#{userId})")
	void insertDefaults(@Param("userId") UUID userId);
}
