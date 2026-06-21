package com.soomgil.auth.application.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 hash와 검증을 담당한다.
 *
 * <p>BCrypt algorithm을 사용한다. raw password는 저장하지 않는다.
 */
@Component
public class PasswordHasher {

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	/**
	 * raw password를 bcrypt hash로 변환한다.
	 *
	 * @param rawPassword 사용자 입력 비밀번호
	 * @return bcrypt hash 문자열
	 */
	public String hash(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

	/**
	 * raw password가 저장된 hash와 일치하는지 검증한다.
	 *
	 * @param rawPassword 사용자 입력 비밀번호
	 * @param hash 저장된 bcrypt hash
	 * @return 일치하면 true
	 */
	public boolean matches(String rawPassword, String hash) {
		return passwordEncoder.matches(rawPassword, hash);
	}
}
