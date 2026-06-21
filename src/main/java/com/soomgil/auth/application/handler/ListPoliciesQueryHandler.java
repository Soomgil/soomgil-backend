package com.soomgil.auth.application.handler;

import com.soomgil.auth.api.dto.PolicyDocument;
import com.soomgil.auth.application.query.ListPoliciesQuery;
import com.soomgil.auth.domain.model.PolicyDocumentModel;
import com.soomgil.auth.infrastructure.persistence.PolicyDocumentMapper;
import com.soomgil.common.cqrs.QueryHandler;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 약관 문서 목록을 조회한다.
 */
@Component
@Transactional(readOnly = true)
public class ListPoliciesQueryHandler implements QueryHandler<ListPoliciesQuery, List<PolicyDocument>> {

	private final PolicyDocumentMapper policyDocumentMapper;

	public ListPoliciesQueryHandler(PolicyDocumentMapper policyDocumentMapper) {
		this.policyDocumentMapper = policyDocumentMapper;
	}

	@Override
	public List<PolicyDocument> handle(ListPoliciesQuery query) {
		return policyDocumentMapper.findAll(query.languageCode(), query.requiredOnly())
			.stream()
			.map(this::toDto)
			.toList();
	}

	private PolicyDocument toDto(PolicyDocumentModel model) {
		return new PolicyDocument(
			model.id(),
			model.policyCode(),
			model.version(),
			model.languageCode(),
			model.title(),
			model.contentUrl() != null ? java.net.URI.create(model.contentUrl()) : null,
			model.contentHash(),
			model.isRequired(),
			OffsetDateTime.ofInstant(model.publishedAt(), ZoneOffset.UTC)
		);
	}
}
