package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.common.cqrs.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 표시 이름을 조회한다.
 *
 * <p>{@code auth.user_profiles}에서 display name을 읽어 반환한다.
 * 프로필이 없으면 기본값 "사용자"를 반환한다.
 * 이 handler는 community 등 다른 모듈이 auth mapper에 직접 접근하지 않도록
 * application 계층 interface 역할을 한다.
 */
@Component
@Transactional(readOnly = true)
public class FindDisplayNameQueryHandler implements QueryHandler<FindDisplayNameQuery, String> {

	private static final String DEFAULT_DISPLAY_NAME = "사용자";

	private final UserProfileMapper userProfileMapper;

	public FindDisplayNameQueryHandler(UserProfileMapper userProfileMapper) {
		this.userProfileMapper = userProfileMapper;
	}

	@Override
	public String handle(FindDisplayNameQuery query) {
		return userProfileMapper.findDisplayName(query.userId())
			.orElse(DEFAULT_DISPLAY_NAME);
	}
}
