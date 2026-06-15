package com.soomgil.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ValidationRulesTest {

	@Test
	void requiresNotBlankString() {
		assertThat(ValidationRules.requireNotBlank("Jeju", "title")).isEqualTo("Jeju");
		assertThatThrownBy(() -> ValidationRules.requireNotBlank(null, "title"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("title");
		assertThatThrownBy(() -> ValidationRules.requireNotBlank(" ", "title"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("title");
	}

	@Test
	void requiresPositiveNumber() {
		assertThat(ValidationRules.requirePositive(1, "size")).isEqualTo(1);
		assertThatThrownBy(() -> ValidationRules.requirePositive(0, "size"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("size");
	}

	@Test
	void requiresNonNegativeNumber() {
		assertThat(ValidationRules.requireNonNegative(0, "offset")).isZero();
		assertThatThrownBy(() -> ValidationRules.requireNonNegative(-1, "offset"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("offset");
	}

	@Test
	void requiresNumberBetweenInclusiveBounds() {
		assertThat(ValidationRules.requireBetween(10, 1, 10, "pageSize")).isEqualTo(10);
		assertThatThrownBy(() -> ValidationRules.requireBetween(11, 1, 10, "pageSize"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("pageSize");
	}
}
