package com.soomgil.trip.infrastructure;

import com.soomgil.trip.application.port.TripInviteCodeGenerator;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 운영용 여행방 초대 code generator.
 *
 * <p>혼동하기 쉬운 문자를 제외한 대문자/숫자 alphabet에서 12자를 생성한다.
 */
@Component
public class InviteCodeGenerator implements TripInviteCodeGenerator {

	private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
	private static final int CODE_LENGTH = 12;
	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public String generate() {
		StringBuilder builder = new StringBuilder(CODE_LENGTH);
		for (int index = 0; index < CODE_LENGTH; index++) {
			builder.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
		}
		return builder.toString();
	}
}
