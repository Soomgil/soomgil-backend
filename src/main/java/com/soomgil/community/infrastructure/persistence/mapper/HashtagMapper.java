package com.soomgil.community.infrastructure.persistence.mapper;

import com.soomgil.community.domain.model.HashtagRecord;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 커뮤니티 해시태그 MyBatis mapper.
 *
 * <p>정규화된 이름으로 upsert하고, 게시글 등록/해제 시 usage_count를 조정한다.
 */
@Mapper
public interface HashtagMapper {

	/**
	 * 신규 해시태그를 등록한다. 이미 존재하면 무시된다.
	 *
	 * @param id 해시태그 식별자
	 * @param name 표시용 이름
	 * @param normalizedName 정규화된 이름 (unique)
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO community.hashtags (id, name, normalized_name, usage_count, created_at, updated_at)
		VALUES (#{id}, #{name}, #{normalizedName}, 0, #{now}, #{now})
		ON CONFLICT (normalized_name) DO NOTHING
		""")
	void insertOrIgnore(
		@Param("id") UUID id,
		@Param("name") String name,
		@Param("normalizedName") String normalizedName,
		@Param("now") Instant now
	);

	/**
	 * 정규화된 이름으로 해시태그를 찾는다.
	 *
	 * @param normalizedName 정규화된 이름
	 * @return 해시태그. 없으면 empty.
	 */
	@Select("""
		SELECT id, name, normalized_name, usage_count, created_at, updated_at
		FROM community.hashtags
		WHERE normalized_name = #{normalizedName}
		""")
	Optional<HashtagRecord> findByNormalizedName(@Param("normalizedName") String normalizedName);

	/**
	 * 정규화된 이름 목록으로 일괄 조회한다.
	 *
	 * @param normalizedNames 정규화된 이름 목록
	 * @return 해시태그 목록
	 */
	@Select("""
		<script>
		SELECT id, name, normalized_name, usage_count, created_at, updated_at
		FROM community.hashtags
		WHERE normalized_name IN
		<foreach item="n" collection="normalizedNames" open="(" separator="," close=")">
			#{n}
		</foreach>
		</script>
		""")
	List<HashtagRecord> findAllByNormalizedNames(@Param("normalizedNames") Collection<String> normalizedNames);

	/**
	 * usage_count를 지정한 수만큼 증가시킨다.
	 *
	 * @param id 해시태그 식별자
	 * @param delta 증감량 (음수 허용, 0 미만으로는 내려가지 않음)
	 * @param now 업데이트 시각
	 */
	@Update("""
		UPDATE community.hashtags
		SET usage_count = GREATEST(usage_count + #{delta}, 0),
		    updated_at = #{now}
		WHERE id = #{id}
		""")
	void adjustUsageCount(@Param("id") UUID id, @Param("delta") int delta, @Param("now") Instant now);
}
