package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.query.FindUserRolesQuery;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.common.cqrs.QueryHandler;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자에게 부여된 역할 목록을 조회한다.
 *
 * <p>{@code auth.user_roles}에서 역할 문자열 목록을 읽어 반환한다.
 * 이 handler는 community 등 다른 모듈이 auth mapper에 직접 접근하지 않도록
 * application 계층 interface 역할을 한다.
 */
@Component
@Transactional(readOnly = true)
public class FindUserRolesQueryHandler implements QueryHandler<FindUserRolesQuery, List<String>> {

	private final UserMapper userMapper;

	public FindUserRolesQueryHandler(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	@Override
	public List<String> handle(FindUserRolesQuery query) {
		return userMapper.findRolesByUserId(query.userId());
	}
}
