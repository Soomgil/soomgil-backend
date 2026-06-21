package com.soomgil.user.infrastructure.persistence;

import com.soomgil.user.domain.model.UserProfileRecord;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * {@code auth.user_profiles} 테이블에서 프로필을 읽고 갱신하는 MyBatis mapper.
 *
 * <p>{@code PATCH /me} 흐름에서 사용한다. 회원가입 시 기본 row를 생성하는 insert 책임은
 * {@code auth.UserProfileMapper}에 그대로 둔다.
 */
@Mapper
public interface UserMeMapper {

	/**
	 * 사용자 식별자로 전체 프로필 row를 조회한다.
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

	/**
	 * 전체 프로필 row를 갱신한다. {@code null} 필드는 {@code NULL}로 덮어쓴다.
	 *
	 * <p>handler는 {@link #findFull}로 현재 값을 읽은 뒤 command의 {@code null}이 아닌 필드를
	 * merge하여 호출한다. {@code profile_image_url}은 media 모듈 연동 전까지는
	 * {@code profile_media_file_id}로부터 파생하지 않고 그대로 둔다.
	 *
	 * @param userId 사용자 식별자
	 * @param displayName 표시 이름
	 * @param profileMediaFileId 프로필 미디어 파일 식별자
	 * @param bio 자기소개
	 * @param profileVisibility 프로필 공개 범위 문자열({@code PUBLIC} 또는 {@code PRIVATE})
	 * @return 갱신된 행 수. 정상 흐름에서 1
	 */
	@Update("""
		UPDATE auth.user_profiles
		SET display_name = #{displayName},
		    profile_image_url = #{profileImageUrl},
		    profile_media_file_id = #{profileMediaFileId},
		    bio = #{bio},
		    profile_visibility = #{profileVisibility},
		    updated_at = now()
		WHERE user_id = #{userId}
		""")
	int update(@Param("userId") UUID userId,
		@Param("displayName") String displayName,
		@Param("profileImageUrl") String profileImageUrl,
		@Param("profileMediaFileId") UUID profileMediaFileId,
		@Param("bio") String bio,
		@Param("profileVisibility") String profileVisibility);

	/**
	 * {@link UserProfileRecord}의 필드를 풀어서 {@link #update}에 전달하는 편의 메서드.
	 *
	 * @param record 갱신할 프로필 값
	 * @return 갱신된 행 수
	 */
	default int updateRecord(UserProfileRecord record) {
		return update(
			record.userId(),
			record.displayName(),
			record.profileImageUrl(),
			record.profileMediaFileId(),
			record.bio(),
			record.profileVisibility() == null ? null : record.profileVisibility().name()
		);
	}
}
