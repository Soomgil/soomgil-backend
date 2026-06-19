package com.soomgil.user.infrastructure.persistence;

import com.soomgil.user.domain.model.UserSummaryRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * {@code auth.user_profiles} 테이블에서 사용자 검색을 담당하는 MyBatis mapper.
 *
 * <p>{@code GET /users} 흐름에서 사용한다. {@code PRIVATE} 프로필은 결과에서 제외한다.
 */
@Mapper
public interface UserSearchMapper {

	/**
	 * 표시 이름 부분 일치 검색을 수행한다.
	 *
	 * <p>{@code query}가 {@code null}이거나 빈 문자열이면 전체 {@code PUBLIC} 프로필을 반환한다.
	 * 정렬은 {@code display_name} 오름차순으로 고정한다(프론트엔드가 임의 정렬을 지원하기 전까지).
	 *
	 * @param query 검색어. {@code null} 또는 빈 값이면 전체
	 * @param limit 반환할 row 수
	 * @param offset 건너뛸 row 수(page * size)
	 * @return 검색 결과 row 목록
	 */
	@Select("""
		<script>
		SELECT user_id, display_name, profile_image_url
		FROM auth.user_profiles
		WHERE profile_visibility = 'PUBLIC'
		<if test="query != null and query != ''">
		  AND display_name ILIKE CONCAT('%', #{query}, '%')
		</if>
		ORDER BY display_name ASC
		LIMIT #{limit} OFFSET #{offset}
		</script>
		""")
	List<UserSummaryRecord> search(@Param("query") String query,
		@Param("limit") int limit,
		@Param("offset") int offset);

	/**
	 * 검색 조건에 일치하는 전체 {@code PUBLIC} 프로필 수를 반환한다.
	 *
	 * @param query 검색어. {@code null} 또는 빈 값이면 전체 개수
	 * @return 검색 결과 전체 개수
	 */
	@Select("""
		<script>
		SELECT COUNT(*)
		FROM auth.user_profiles
		WHERE profile_visibility = 'PUBLIC'
		<if test="query != null and query != ''">
		  AND display_name ILIKE CONCAT('%', #{query}, '%')
		</if>
		</script>
		""")
	long count(@Param("query") String query);
}
