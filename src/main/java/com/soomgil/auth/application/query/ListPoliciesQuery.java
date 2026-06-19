package com.soomgil.auth.application.query;

import com.soomgil.auth.api.dto.PolicyDocument;
import com.soomgil.common.cqrs.Query;
import java.util.List;

/**
 * 약관 문서 목록 조회 요청.
 *
 * @param languageCode 언어 코드 (nullable)
 * @param requiredOnly 필수 약관만 조회할지 여부
 */
public record ListPoliciesQuery(String languageCode, boolean requiredOnly) implements Query<List<PolicyDocument>> {
}
