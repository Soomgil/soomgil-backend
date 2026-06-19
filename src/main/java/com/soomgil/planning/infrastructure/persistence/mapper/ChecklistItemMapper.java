package com.soomgil.planning.infrastructure.persistence.mapper;

import com.soomgil.planning.domain.model.ChecklistItemRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

/**
 * planning.checklist_items 테이블 접근 mapper.
 *
 * <p>모든 UPDATE/DELETE는 {@code WHERE id = ? AND version = ? AND deleted_at IS NULL} 조건으로
 * 낙관적 잠금을 검증한다. reorder는 per-item version check loop를 {@code @Transactional}로 묶어
 * 원자성을 보장한다.
 */
@Mapper
public interface ChecklistItemMapper {

	/**
	 * 신규 item을 INSERT한다.
	 *
	 * @param id item 식별자
	 * @param checklistId 소속 checklist
	 * @param sortOrder 정렬 순서
	 * @param content 본문
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO planning.checklist_items (
		    id, checklist_id, sort_order, content, version, created_at, updated_at
		) VALUES (
		    #{id}, #{checklistId}, #{sortOrder}, #{content}, 1, #{now}, #{now}
		)
		""")
	void insert(
		@Param("id") UUID id,
		@Param("checklistId") UUID checklistId,
		@Param("sortOrder") int sortOrder,
		@Param("content") String content,
		@Param("now") Instant now
	);

	/**
	 * 식별자로 item을 조회한다. 삭제된 row도 포함.
	 *
	 * @param id item 식별자
	 * @return item. 없으면 empty
	 */
	@Select("""
		SELECT id, checklist_id, sort_order, content, version,
		       deleted_at, created_at, updated_at
		FROM planning.checklist_items
		WHERE id = #{id}
		""")
	Optional<ChecklistItemRecord> findById(@Param("id") UUID id);

	/**
	 * checklist의 활성 item을 sort_order 순으로 조회한다.
	 *
	 * @param checklistId checklist 식별자
	 * @return 활성 item 목록 (sort_order ASC)
	 */
	@Select("""
		SELECT id, checklist_id, sort_order, content, version,
		       deleted_at, created_at, updated_at
		FROM planning.checklist_items
		WHERE checklist_id = #{checklistId} AND deleted_at IS NULL
		ORDER BY sort_order ASC
		""")
	List<ChecklistItemRecord> findByChecklistId(@Param("checklistId") UUID checklistId);

	/**
	 * checklist의 활성 item 중 최대 sort_order를 조회한다.
	 *
	 * @param checklistId checklist 식별자
	 * @return 최대 sort_order. 활성 item이 없으면 null
	 */
	@Select("""
		SELECT MAX(sort_order) FROM planning.checklist_items
		WHERE checklist_id = #{checklistId} AND deleted_at IS NULL
		""")
	Integer findMaxSortOrder(@Param("checklistId") UUID checklistId);

	/**
	 * item의 content/sortOrder를 갱신하고 version을 1 증가시킨다.
	 * null content/sortOrder는 SQL COALESCE로 기존값 유지.
	 *
	 * @param id item 식별자
	 * @param content 새 content (null이면 기존값 유지)
	 * @param sortOrder 새 sortOrder (null이면 기존값 유지)
	 * @param baseVersion 호출자가 읽은 version
	 * @param now 수정 시각
	 * @return 영향받은 row 수. 0이면 버전 충돌
	 */
	@Update("""
		UPDATE planning.checklist_items
		SET content = COALESCE(#{content}, content),
		    sort_order = COALESCE(#{sortOrder}, sort_order),
		    version = version + 1,
		    updated_at = #{now}
		WHERE id = #{id} AND version = #{baseVersion} AND deleted_at IS NULL
		""")
	int update(
		@Param("id") UUID id,
		@Param("content") String content,
		@Param("sortOrder") Integer sortOrder,
		@Param("baseVersion") long baseVersion,
		@Param("now") Instant now
	);

	/**
	 * item의 sortOrder만 갱신하고 version을 1 증가시킨다. reorder용.
	 *
	 * @param id item 식별자
	 * @param sortOrder 새 sortOrder
	 * @param baseVersion 호출자가 읽은 version
	 * @param now 수정 시각
	 * @return 영향받은 row 수. 0이면 버전 충돌
	 */
	@Update("""
		UPDATE planning.checklist_items
		SET sort_order = #{sortOrder}, version = version + 1, updated_at = #{now}
		WHERE id = #{id} AND version = #{baseVersion} AND deleted_at IS NULL
		""")
	int updateSortOrder(
		@Param("id") UUID id,
		@Param("sortOrder") int sortOrder,
		@Param("baseVersion") long baseVersion,
		@Param("now") Instant now
	);

	/**
	 * item을 soft delete한다.
	 *
	 * @param id item 식별자
	 * @param baseVersion 호출자가 읽은 version
	 * @param now 삭제 시각
	 * @return 영향받은 row 수. 0이면 버전 충돌
	 */
	@Update("""
		UPDATE planning.checklist_items
		SET deleted_at = #{now}, version = version + 1, updated_at = #{now}
		WHERE id = #{id} AND version = #{baseVersion} AND deleted_at IS NULL
		""")
	int softDelete(
		@Param("id") UUID id,
		@Param("baseVersion") long baseVersion,
		@Param("now") Instant now
	);

	/**
	 * checklist의 모든 활성 item을 soft delete한다. checklist 삭제 cascade용.
	 * version 검증 없이 일괄 처리한다.
	 *
	 * @param checklistId checklist 식별자
	 * @param now 삭제 시각
	 */
	@Update("""
		UPDATE planning.checklist_items
		SET deleted_at = #{now}, version = version + 1, updated_at = #{now}
		WHERE checklist_id = #{checklistId} AND deleted_at IS NULL
		""")
	void softDeleteByChecklistId(
		@Param("checklistId") UUID checklistId,
		@Param("now") Instant now
	);
}
