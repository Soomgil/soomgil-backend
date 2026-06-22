package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiPlainTextFormatterTest {

	@Test
	void removesCommonMarkdownSyntax() {
		String markdown = """
			## 추천 일정
			- **경복궁** 방문
			- [지도 보기](https://example.com)
			""";

		assertThat(AiPlainTextFormatter.format(markdown))
			.isEqualTo("추천 일정\n경복궁 방문\n지도 보기");
	}
}
