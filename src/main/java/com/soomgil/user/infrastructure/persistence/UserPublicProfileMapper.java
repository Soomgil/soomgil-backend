package com.soomgil.user.infrastructure.persistence;

import com.soomgil.user.domain.model.UserProfileRecord;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 단일 사용자의 공개 프로필을 조회하는 MyBatis mapper.
 *
 * <p>{@code GET /users/{userId}} 흐름에서 사용한다. {@code profile_visibility}를 포함한
 * 전체 row를 읽어오며, 호출부(handler)에서 가시성에 따라 응답 범위를 제한한다.
 */
@Mapper
public interface UserPublicProfileMapper {

	/**
	 * 사용자 식별자로 전체 프로필 row를 조회한다.
	 *
	 * <p>{@code targetUserId}에 해당하는 row가 없으면 {@link Optional#empty()}를 반환하며,
	 * 호출부에서 {@code USER_NOT_FOUND} 예외로 변환한다.
	 *
	 * @param targetUserId 조회 대상 사용자 식별자
	 * @return 프로필 row. 없으면 empty
	 */
	@Select("""
		SELECT user_id, display_name, profile_image_url, profile_media_file_id, bio, profile_visibility
		FROM auth.user_profiles
		WHERE user_id = #{targetUserId}
		""")
	Optional<UserProfileRecord> findByUserId(@Param("targetUserId") UUID targetUserId);
}
