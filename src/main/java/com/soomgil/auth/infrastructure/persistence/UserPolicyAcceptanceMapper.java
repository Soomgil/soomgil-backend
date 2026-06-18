package com.soomgil.auth.infrastructure.persistence;

import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * auth.user_policy_acceptances 테이블에 대한 MyBatis mapper.
 */
@Mapper
public interface UserPolicyAcceptanceMapper {

	@Insert("""
		INSERT INTO auth.user_policy_acceptances (user_id, policy_document_id)
		VALUES (#{userId}, #{policyDocumentId})
		""")
	void insert(
		@Param("userId") UUID userId,
		@Param("policyDocumentId") UUID policyDocumentId
	);
}
