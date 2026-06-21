package com.soomgil.auth.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.api.dto.PolicyDocument;
import com.soomgil.auth.application.query.ListPoliciesQuery;
import com.soomgil.auth.domain.model.PolicyDocumentModel;
import com.soomgil.auth.infrastructure.persistence.PolicyDocumentMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListPoliciesQueryHandlerTest {

	private final PolicyDocumentMapper policyDocumentMapper = mock(PolicyDocumentMapper.class);

	private final ListPoliciesQueryHandler handler = new ListPoliciesQueryHandler(policyDocumentMapper);

	private PolicyDocumentModel model(String policyCode, String languageCode, boolean required) {
		return new PolicyDocumentModel(
			UUID.randomUUID(), policyCode, "1.0.0", languageCode, policyCode + " title",
			"https://example.com/" + policyCode, "hash-" + policyCode, required, Instant.now()
		);
	}

	@Test
	@DisplayName("전체 약관 목록을 languageCode 필터 없이 조회한다")
	void listsAllPoliciesWithoutFilters() {
		when(policyDocumentMapper.findAll(eq(null), eq(false))).thenReturn(List.of(
			model("TERMS_OF_SERVICE", "ko", true),
			model("PRIVACY_POLICY", "ko", false)
		));

		List<PolicyDocument> result = handler.handle(new ListPoliciesQuery(null, false));

		assertThat(result).hasSize(2);
		assertThat(result.get(0).policyCode()).isEqualTo("TERMS_OF_SERVICE");
		assertThat(result.get(0).isRequired()).isTrue();
		assertThat(result.get(0).contentUrl()).hasToString("https://example.com/TERMS_OF_SERVICE");
		verify(policyDocumentMapper).findAll(null, false);
	}

	@Test
	@DisplayName("requiredOnly=true면 필수 약관만 조회한다")
	void listsRequiredPoliciesOnly() {
		when(policyDocumentMapper.findAll(eq(null), eq(true))).thenReturn(List.of(
			model("TERMS_OF_SERVICE", "ko", true)
		));

		List<PolicyDocument> result = handler.handle(new ListPoliciesQuery(null, true));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isRequired()).isTrue();
		verify(policyDocumentMapper).findAll(null, true);
	}

	@Test
	@DisplayName("languageCode 필터를 mapper에 전달한다")
	void appliesLanguageCodeFilter() {
		when(policyDocumentMapper.findAll(eq("en"), anyBoolean())).thenReturn(List.of(
			model("TERMS_OF_SERVICE", "en", true)
		));

		List<PolicyDocument> result = handler.handle(new ListPoliciesQuery("en", false));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).languageCode()).isEqualTo("en");
		verify(policyDocumentMapper).findAll("en", false);
	}

	@Test
	@DisplayName("contentUrl이 null이면 DTO의 contentUrl도 null이다")
	void mapsNullContentUrlToDto() {
		PolicyDocumentModel nullUrlModel = new PolicyDocumentModel(
			UUID.randomUUID(), "TERMS", "1.0", "ko", "title", null, "hash", true, Instant.now()
		);
		when(policyDocumentMapper.findAll(eq(null), anyBoolean())).thenReturn(List.of(nullUrlModel));

		List<PolicyDocument> result = handler.handle(new ListPoliciesQuery(null, false));

		assertThat(result.get(0).contentUrl()).isNull();
	}
}
