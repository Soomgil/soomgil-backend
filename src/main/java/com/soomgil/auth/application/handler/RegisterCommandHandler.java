package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.RegisterCommand;
import com.soomgil.auth.application.command.RegisterResult;
import com.soomgil.auth.application.service.EmailVerificationService;
import com.soomgil.auth.application.service.PasswordHasher;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.PasswordCredentialMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.auth.infrastructure.persistence.UserSettingsMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일/비밀번호 회원가입을 처리한다.
 *
 * <p>{@code PENDING} 상태로 계정을 생성하고 인증 메일을 발송한다. 사용자가
 * {@code /auth/email-verifications}로 토큰을 제출해 인증을 완료하면 그제야
 * {@code ACTIVE}로 전환된다({@link VerifyEmailCommandHandler}). 따라서 이
 * handler는 access/refresh token을 발급하지 않는다.
 *
 * <p>생성 순서: {@code users}(PENDING) → {@code user_email_addresses}(verifiedAt=null)
 * → {@code user_profiles} → {@code user_settings} → {@code user_password_credentials}.
 * 그 후 이메일 인증 토큰을 발급하고 메일을 발송한다.
 */
@Component
@Transactional
public class RegisterCommandHandler implements CommandHandler<RegisterCommand, RegisterResult> {

	private final UserMapper userMapper;
	private final EmailAddressMapper emailAddressMapper;
	private final PasswordCredentialMapper passwordCredentialMapper;
	private final UserProfileMapper userProfileMapper;
	private final UserSettingsMapper userSettingsMapper;
	private final PasswordHasher passwordHasher;
	private final EmailVerificationService emailVerificationService;

	public RegisterCommandHandler(
		UserMapper userMapper,
		EmailAddressMapper emailAddressMapper,
		PasswordCredentialMapper passwordCredentialMapper,
		UserProfileMapper userProfileMapper,
		UserSettingsMapper userSettingsMapper,
		PasswordHasher passwordHasher,
		EmailVerificationService emailVerificationService
	) {
		this.userMapper = userMapper;
		this.emailAddressMapper = emailAddressMapper;
		this.passwordCredentialMapper = passwordCredentialMapper;
		this.userProfileMapper = userProfileMapper;
		this.userSettingsMapper = userSettingsMapper;
		this.passwordHasher = passwordHasher;
		this.emailVerificationService = emailVerificationService;
	}

	@Override
	public RegisterResult handle(RegisterCommand command) {
		String normalizedEmail = EmailAddress.normalize(command.email());

		if (emailAddressMapper.existsActiveByNormalizedEmail(normalizedEmail)) {
			throw new AuthException(ErrorCode.EMAIL_ALREADY_USED);
		}

		UUID userId = UUID.randomUUID();

		userMapper.insert(userId, UserStatus.PENDING.name());
		emailAddressMapper.insertPrimary(userId, command.email(), normalizedEmail, null);
		userProfileMapper.insert(userId, command.displayName());
		userSettingsMapper.insertDefaults(userId);
		passwordCredentialMapper.insert(userId, passwordHasher.hash(command.password()));

		// 인증 메일 발송을 위해 방금 생성한 email_address row의 id를 조회한다.
		UUID emailAddressId = emailAddressMapper.findActiveByNormalizedEmail(normalizedEmail)
			.orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND))
			.id();
		emailVerificationService.issueAndSend(emailAddressId, command.email());

		return new RegisterResult(userId, command.email());
	}
}
