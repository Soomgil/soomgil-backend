package com.soomgil.global.storage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 업로드 object의 magic bytes로 지원 media type을 식별한다.
 *
 * <p>클라이언트가 지정한 {@code Content-Type}만 신뢰하지 않고 완료 등록 시 교차 검증하는 데 사용한다.
 */
public final class MediaContentInspector {

	private static final byte[] PNG = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

	public String detect(byte[] content) {
		if (content == null) {
			return null;
		}
		if (content.length >= 3
			&& content[0] == (byte) 0xff
			&& content[1] == (byte) 0xd8
			&& content[2] == (byte) 0xff) {
			return "image/jpeg";
		}
		if (content.length >= PNG.length && Arrays.equals(PNG, Arrays.copyOf(content, PNG.length))) {
			return "image/png";
		}
		if (content.length >= 8
			&& "ftyp".equals(new String(content, 4, 4, StandardCharsets.US_ASCII))) {
			return "video/mp4";
		}
		return null;
	}
}
