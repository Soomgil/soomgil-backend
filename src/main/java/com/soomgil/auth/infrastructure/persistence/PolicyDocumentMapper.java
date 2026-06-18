package com.soomgil.auth.infrastructure.persistence;

import com.soomgil.auth.domain.model.PolicyDocumentModel;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * auth.policy_documents 테이블에 대한 MyBatis mapper.
 */
@Mapper
public interface PolicyDocumentMapper {

	@Select("""
		<script>
		SELECT id, policy_code, version, language_code, title, content_url, content_hash,
		       is_required, published_at
		FROM auth.policy_documents
		WHERE retired_at IS NULL
		<if test="languageCode != null">AND language_code = #{languageCode}</if>
		<if test="requiredOnly">AND is_required = true</if>
		ORDER BY is_required DESC, policy_code ASC
		</script>
		""")
	List<PolicyDocumentModel> findAll(
		@Param("languageCode") String languageCode,
		@Param("requiredOnly") boolean requiredOnly
	);

	@Select("""
		<script>
		SELECT id, policy_code, version, language_code, title, content_url, content_hash,
		       is_required, published_at
		FROM auth.policy_documents
		WHERE id IN
		<foreach item="id" collection="ids" open="(" separator="," close=")">#{id}</foreach>
		AND retired_at IS NULL
		</script>
		""")
	List<PolicyDocumentModel> findByIds(@Param("ids") List<UUID> ids);

	@Select("SELECT COUNT(*) FROM auth.policy_documents WHERE is_required = true AND retired_at IS NULL")
	int countRequired();
}
