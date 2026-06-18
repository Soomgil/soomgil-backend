package com.soomgil.user.infrastructure.persistence;

import com.soomgil.user.domain.model.UserSettingsRecord;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * {@code auth.user_settings} 테이블에서 user 도메인이 사용하는 읽기/쓰기 mapper.
 *
 * <p>user 도메인의 {@code GET /me/settings}, {@code PATCH /me/settings},
 * {@code PATCH /me} 응답 조립에서 공통으로 사용한다. 회원가입 시 기본 row를 생성하는
 * insert 책임은 {@code auth} 모듈의 {@code UserSettingsMapper}에 그대로 둔다.
 */
@Mapper
public interface UserMeSettingsMapper {

	/**
	 * 사용자 식별자로 설정 row를 조회한다.
	 *
	 * <p>설정 row는 회원가입 시 기본값과 함께 생성되므로, 정상 흐름에서는 항상 존재한다.
	 * 없으면 {@link Optional#empty()}를 반환하며, 호출부에서 기본값으로 대체하거나
	 * {@code USER_NOT_FOUND}/{@code PROFILE_NOT_FOUND}로 변환한다.
	 *
	 * @param userId 사용자 식별자
	 * @return 설정 row. 없으면 empty
	 */
	@Select("""
		SELECT user_id, display_language, timezone,
		       marketing_email_opt_in, marketing_email_opted_in_at, marketing_email_opted_out_at,
		       trip_invite_email_opt_in
		FROM auth.user_settings
		WHERE user_id = #{userId}
		""")
	Optional<UserSettingsRecord> findByUserId(@Param("userId") UUID userId);

	/**
	 * 사용자 설정 row를 전체 덮어쓴다.
	 *
	 * <p>회원가입 시 row가 항상 생성되므로, 정상 흐름에서는 항상 1행이 갱신된다.
	 * 갱신된 행이 0이면 row가 없는 비정상 상태로, 호출부에서 {@code PROFILE_NOT_FOUND}로 변환한다.
	 *
	 * @param userId 사용자 식별자
	 * @param displayLanguage 표시 언어 코드
	 * @param timezone 타임존
	 * @param marketingEmailOptIn 마케팅 이메일 수신 동의 여부
	 * @param marketingEmailOptedInAt 마케팅 동의 시각
	 * @param marketingEmailOptedOutAt 마케팅 동의 철회 시각
	 * @param tripInviteEmailOptIn 여행방 초대 이메일 수신 동의 여부
	 * @return 갱신된 행 수. 정상 흐름에서 1
	 */
	@Update("""
		UPDATE auth.user_settings
		SET display_language = #{displayLanguage},
		    timezone = #{timezone},
		    marketing_email_opt_in = #{marketingEmailOptIn},
		    marketing_email_opted_in_at = #{marketingEmailOptedInAt},
		    marketing_email_opted_out_at = #{marketingEmailOptedOutAt},
		    trip_invite_email_opt_in = #{tripInviteEmailOptIn},
		    updated_at = now()
		WHERE user_id = #{userId}
		""")
	int update(@Param("userId") UUID userId,
		@Param("displayLanguage") String displayLanguage,
		@Param("timezone") String timezone,
		@Param("marketingEmailOptIn") boolean marketingEmailOptIn,
		@Param("marketingEmailOptedInAt") java.time.OffsetDateTime marketingEmailOptedInAt,
		@Param("marketingEmailOptedOutAt") java.time.OffsetDateTime marketingEmailOptedOutAt,
		@Param("tripInviteEmailOptIn") boolean tripInviteEmailOptIn);

	/**
	 * {@link UserSettingsRecord}의 필드를 풀어서 {@link #update}에 전달하는 편의 메서드.
	 *
	 * @param userId 사용자 식별자
	 * @param record 갱신할 설정 값
	 * @return 갱신된 행 수
	 */
	default int updateRecord(UUID userId, UserSettingsRecord record) {
		return update(
			userId,
			record.displayLanguage(),
			record.timezone(),
			record.marketingEmailOptIn(),
			record.marketingEmailOptedInAt(),
			record.marketingEmailOptedOutAt(),
			record.tripInviteEmailOptIn()
		);
	}
}
