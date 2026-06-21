package com.soomgil.auth.infrastructure.persistence;

import com.soomgil.user.domain.model.UserProfileRecord;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

	/**
	 * 사용자 식별자로 전체 프로필 row를 조회한다.
	 *
	 * <p>{@code GET /me} 응답에 bio/visibility/프로필 미디어까지 모두 담기 위해 사용한다.
	 * 기존 {@link #findDisplayName}은 display_name만 필요한 회원가입 흐름이 쓴다.
	 *
	 * @param userId 사용자 식별자
	 * @return 프로필 row. 없으면 empty
	 */
	@Select("""
		SELECT user_id, display_name, profile_image_url, profile_media_file_id, bio, profile_visibility
		FROM auth.user_profiles
		WHERE user_id = #{userId}
		""")
	Optional<UserProfileRecord> findFull(@Param("userId") UUID userId);

	@Update("UPDATE auth.user_profiles SET display_name = #{displayName} WHERE user_id = #{userId}")
	int updateDisplayName(@Param("userId") UUID userId, @Param("displayName") String displayName);
}
