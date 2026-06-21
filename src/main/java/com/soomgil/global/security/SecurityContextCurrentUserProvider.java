package com.soomgil.global.security;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security context의 인증 principal을 application 계층에 제공한다.
 *
 * <p>JWT filter가 만든 {@link CurrentUser}만 허용하며, 인증이 없거나 다른 principal이면
 * {@link ErrorCode#UNAUTHORIZED}로 거부한다.
 */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

	@Override
	public CurrentUser currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
			|| !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return currentUser;
	}
}
