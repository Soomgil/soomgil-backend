package com.soomgil.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

	@Test
	void createsPageResponseFromSpringPage() {
		PageImpl<String> page = new PageImpl<>(
			List.of("a", "b"),
			PageRequest.of(2, 2),
			7
		);

		PageResponse<String> response = PageResponse.from(page);

		assertThat(response.items()).containsExactly("a", "b");
		assertThat(response.page().page()).isEqualTo(2);
		assertThat(response.page().size()).isEqualTo(2);
		assertThat(response.page().totalElements()).isEqualTo(7);
		assertThat(response.page().totalPages()).isEqualTo(4);
	}
}
