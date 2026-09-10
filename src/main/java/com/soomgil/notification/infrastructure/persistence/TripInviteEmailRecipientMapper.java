package com.soomgil.notification.infrastructure.persistence;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 여행 초대 이메일 수신에 동의한 사용자의 인증된 기본 이메일을 조회한다. */
@Mapper
public interface TripInviteEmailRecipientMapper {

	@Select("""
		SELECT email.email
		FROM auth.user_email_addresses email
		JOIN auth.user_settings settings ON settings.user_id = email.user_id
		JOIN auth.users users ON users.id = email.user_id
		WHERE email.user_id = #{userId}
		  AND email.is_primary = true
		  AND email.verified_at IS NOT NULL
		  AND email.removed_at IS NULL
		  AND settings.trip_invite_email_opt_in = true
		  AND users.deleted_at IS NULL
		LIMIT 1
		""")
	String findOptedInVerifiedEmail(@Param("userId") UUID userId);
}
