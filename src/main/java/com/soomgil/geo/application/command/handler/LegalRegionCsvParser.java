package com.soomgil.geo.application.command.handler;

import com.soomgil.geo.application.port.LegalRegionUpsert;
import com.soomgil.geo.domain.model.LegalRegionLevel;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 정부 법정동 코드 CSV/TSV를 application upsert 모델로 변환한다.
 */
final class LegalRegionCsvParser {

	private static final String ACTIVE_STATUS = "존재";

	private LegalRegionCsvParser() {
	}

	static List<LegalRegionUpsert> parse(String content, Instant syncedAt) {
		if (content == null || content.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Legal region CSV must not be empty.");
		}
		String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
		String[] lines = normalized.split("\n");
		if (lines.length < 2) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Legal region CSV must include a header and rows.");
		}

		List<LegalRegionUpsert> regions = new ArrayList<>();
		for (int index = 1; index < lines.length; index++) {
			String line = lines[index].trim();
			if (line.isEmpty()) {
				continue;
			}
			regions.add(parseLine(line, index + 1, syncedAt));
		}
		return List.copyOf(regions);
	}

	private static LegalRegionUpsert parseLine(String line, int lineNumber, Instant syncedAt) {
		String[] columns = splitColumns(line);
		if (columns.length < 3) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Legal region CSV row is invalid: line " + lineNumber);
		}
		String code = columns[0].replace("\uFEFF", "").trim();
		String fullName = columns[1].trim();
		String rawStatus = columns[2].trim();
		validateCode(code, lineNumber);
		if (fullName.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Legal region name is required: line " + lineNumber);
		}
		if (rawStatus.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Legal region status is required: line " + lineNumber);
		}

		LegalRegionLevel level = levelOf(code);
		return new LegalRegionUpsert(
			code,
			lastName(fullName),
			fullName,
			level,
			parentCode(code, level),
			code.substring(0, 2),
			level == LegalRegionLevel.SIDO ? null : code.substring(0, 5),
			level == LegalRegionLevel.EUPMYEONDONG ? code.substring(0, 8) : null,
			rawStatus,
			ACTIVE_STATUS.equals(rawStatus),
			syncedAt
		);
	}

	private static String[] splitColumns(String line) {
		String[] tabColumns = line.split("\t", -1);
		if (tabColumns.length >= 3) {
			return tabColumns;
		}
		return line.split(",", -1);
	}

	private static void validateCode(String code, int lineNumber) {
		if (!code.matches("\\d{10}")) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Legal region code must be 10 digits: line " + lineNumber);
		}
	}

	private static LegalRegionLevel levelOf(String code) {
		if (code.substring(2).equals("00000000")) {
			return LegalRegionLevel.SIDO;
		}
		if (code.substring(5).equals("00000")) {
			return LegalRegionLevel.SIGUNGU;
		}
		return LegalRegionLevel.EUPMYEONDONG;
	}

	private static String parentCode(String code, LegalRegionLevel level) {
		return switch (level) {
			case SIDO -> null;
			case SIGUNGU -> code.substring(0, 2) + "00000000";
			case EUPMYEONDONG -> code.substring(0, 5) + "00000";
		};
	}

	private static String lastName(String fullName) {
		String[] names = fullName.trim().split("\\s+");
		return names[names.length - 1];
	}
}
