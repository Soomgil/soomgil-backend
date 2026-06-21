package com.soomgil.auth.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.soomgil.auth.infrastructure.persistence.UserPolicyAcceptanceMapper;
import com.soomgil.global.error.ErrorCode;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegisterCommandHandlerTest {

	private final UserMapper userMapper = mock(UserMapper.class);
	private final EmailAddressMapper emailAddressMapper = mock(EmailAddressMapper.class);
	private final PasswordCredentialMapper passwordCredentialMapper = mock(PasswordCredentialMapper.class);
	private final UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
	private final UserSettingsMapper userSettingsMapper = mock(UserSettingsMapper.class);
	private final UserPolicyAcceptanceMapper userPolicyAcceptanceMapper = mock(UserPolicyAcceptanceMapper.class);
	private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
	private final EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);

	private final RegisterCommandHandler handler = new RegisterCommandHandler(
		userMapper,
		emailAddressMapper,
		passwordCredentialMapper,
		userProfileMapper,
		userSettingsMapper,
		userPolicyAcceptanceMapper,
		passwordHasher,
		emailVerificationService
	);

	@Test
	@DisplayName("정상 가입 시 PENDING 계정을 만들고 인증 메일을 발송하며 토큰은 발급하지 않는다")
	void registersNewUserAsPendingAndSendsVerification() {
		String hashedPassword = "bcrypt-hash";
		UUID emailId = UUID.randomUUID();
		EmailAddress emailAddress = new EmailAddress(
			emailId, null, "user@example.com", "user@example.com", true, null
		);
		when(emailAddressMapper.existsActiveByNormalizedEmail(anyString())).thenReturn(false);
		when(passwordHasher.hash(anyString())).thenReturn(hashedPassword);
		when(emailAddressMapper.findActiveByNormalizedEmail("user@example.com"))
			.thenReturn(Optional.of(emailAddress));

		RegisterResult result = handler.handle(new RegisterCommand(
			"user@example.com", "P@ssw0rd!", "민지", List.of()
		));

		assertThat(result.email()).isEqualTo("user@example.com");
		assertThat(result.userId()).isNotNull();

		verify(userMapper).insert(any(UUID.class), eq(UserStatus.PENDING.name()));
		verify(emailAddressMapper).insertPrimary(
			any(UUID.class), eq("user@example.com"), eq("user@example.com"), isNull()
		);
		verify(userProfileMapper).insert(any(UUID.class), eq("민지"));
		verify(userSettingsMapper).insertDefaults(any(UUID.class));
		verify(passwordCredentialMapper).insert(any(UUID.class), eq(hashedPassword));
		verify(emailVerificationService).issueAndSend(eq(emailId), eq("user@example.com"));
	}

	@Test
	@DisplayName("이메일을 소문자로 정규화하여 중복 확인에 사용한다")
	void normalizesEmailBeforeDuplicateCheck() {
		UUID emailId = UUID.randomUUID();
		EmailAddress emailAddress = new EmailAddress(
			emailId, null, "user@example.com", "user@example.com", true, null
		);
		when(emailAddressMapper.existsActiveByNormalizedEmail(eq("user@example.com"))).thenReturn(false);
		when(passwordHasher.hash(anyString())).thenReturn("hash");
		when(emailAddressMapper.findActiveByNormalizedEmail("user@example.com"))
			.thenReturn(Optional.of(emailAddress));

		handler.handle(new RegisterCommand("USER@Example.COM", "pw", "이름", List.of()));

		verify(emailAddressMapper).existsActiveByNormalizedEmail("user@example.com");
	}

	@Test
	@DisplayName("이미 사용 중인 이메일은 EMAIL_ALREADY_USED 예외를 던진다")
	void throwsWhenEmailAlreadyUsed() {
		when(emailAddressMapper.existsActiveByNormalizedEmail(anyString())).thenReturn(true);

		assertThatThrownBy(() -> handler.handle(new RegisterCommand(
			"user@example.com", "P@ssw0rd!", "민지", List.of()
		)))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.EMAIL_ALREADY_USED);

		verify(userMapper, never()).insert(any(UUID.class), anyString());
		verify(passwordCredentialMapper, never()).insert(any(UUID.class), anyString());
		verify(emailVerificationService, never()).issueAndSend(any(UUID.class), anyString());
	}
}
