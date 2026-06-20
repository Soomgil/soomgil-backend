package com.soomgil.geo.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 법정동 CSV 동기화 command.
 */
public record SyncLegalRegionsCommand(
	String source,
	String sourceFileName,
	byte[] content,
	Charset charset
) implements Command<SyncLegalRegionsResult> {

	public SyncLegalRegionsCommand {
		source = normalizeSource(source);
		sourceFileName = normalizeText(sourceFileName);
		content = content == null ? new byte[0] : content.clone();
		charset = charset == null ? StandardCharsets.UTF_8 : charset;
	}

	@Override
	public byte[] content() {
		return content.clone();
	}

	private static String normalizeSource(String value) {
		String normalized = normalizeText(value);
		return normalized == null ? "dump" : normalized;
	}

	private static String normalizeText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
