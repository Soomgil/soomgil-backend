package com.soomgil.ai.application;

import java.util.regex.Pattern;

/** AI 응답에서 UI에 노출하면 안 되는 Markdown 문법을 제거한다. */
final class AiPlainTextFormatter {

	static final String UNSUPPORTED_NOTICE = "현재 기능으로는 직접 처리할 수 없어요.";
	private static final Pattern LINK = Pattern.compile("!?\\[([^]\\r\\n]+)]\\([^\\r\\n)]+\\)");
	private static final Pattern HEADING = Pattern.compile("(?m)^\\s*#{1,6}\\s*");
	private static final Pattern LIST_MARKER = Pattern.compile("(?m)^\\s*(?:>|[-+*]|\\d+[.)])\\s+");
	private static final Pattern TABLE_SEPARATOR = Pattern.compile("(?m)^\\s*\\|?(?:\\s*:?-{3,}:?\\s*\\|)+\\s*$");

	private AiPlainTextFormatter() {
	}

	static String format(String content) {
		if (content == null) return "";
		String plain = content
			.replaceAll("(?s)```(?:[a-zA-Z0-9_-]+)?\\s*(.*?)```", "$1")
			.replace("`", "");
		plain = LINK.matcher(plain).replaceAll("$1");
		plain = TABLE_SEPARATOR.matcher(plain).replaceAll("");
		plain = HEADING.matcher(plain).replaceAll("");
		plain = LIST_MARKER.matcher(plain).replaceAll("");
		plain = plain.replaceAll("(\\*\\*|__|~~)", "")
			.replaceAll("(?<!\\*)\\*([^*\\r\\n]+)\\*(?!\\*)", "$1")
			.replaceAll("(?m)^\\s*\\|\\s*|\\s*\\|\\s*$", "")
			.replaceAll("[ \\t]+", " ")
			.replaceAll("\\s*\\R\\s*", "\n")
			.replaceAll("\\n{3,}", "\n\n")
			.trim();
		return plain;
	}
}
