package com.soomgil.user.application.query;

import com.soomgil.common.cqrs.Query;
import java.util.UUID;

/**
 * 현재 로그인 사용자의 설정 조회 요청.
 *
 * <p>{@link com.soomgil.user.api.dto.UserSettings} 응답을 생성하기 위해
 * {@code auth.user_settings} row를 읽어온다.
 *
 * @param userId 현재 로그인 사용자 식별자
 */
public record GetMySettingsQuery(
	UUID userId
) implements Query<com.soomgil.user.api.dto.UserSettings> {
}
