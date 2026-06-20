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
 * <p>DBML에 version 컬럼이 없으므로 UPDATE/DELETE는 식별자 기반으로 처리한다.
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
	 * @param actorUserId 작성자
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO planning.checklist_items (
		    id, checklist_id, sort_order, content,
		    created_by_user_id, updated_by_user_id, created_at, updated_at
		) VALUES (
		    #{id}, #{checklistId}, #{sortOrder}, #{content},
		    #{actorUserId}, #{actorUserId}, #{now}, #{now}
		)
		""")
	void insert(
		@Param("id") UUID id,
		@Param("checklistId") UUID checklistId,
		@Param("sortOrder") int sortOrder,
		@Param("content") String content,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);

	/**
	 * 식별자로 item을 조회한다. 삭제된 row도 포함.
	 *
	 * @param id item 식별자
	 * @return item. 없으면 empty
	 */
	@Select("""
		SELECT id, checklist_id, sort_order, content,
		       created_by_user_id, updated_by_user_id, deleted_by_user_id,
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
		SELECT id, checklist_id, sort_order, content,
		       created_by_user_id, updated_by_user_id, deleted_by_user_id,
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
	 * item의 content/sortOrder를 갱신한다.
	 * null content/sortOrder는 SQL COALESCE로 기존값 유지.
	 *
	 * @param id item 식별자
	 * @param content 새 content (null이면 기존값 유지)
	 * @param sortOrder 새 sortOrder (null이면 기존값 유지)
	 * @param actorUserId 수정자
	 * @param now 수정 시각
	 * @return 영향받은 row 수. 0이면 대상이 없거나 이미 삭제됨
	 */
	@Update("""
		UPDATE planning.checklist_items
		SET content = COALESCE(#{content}, content),
		    sort_order = COALESCE(#{sortOrder}, sort_order),
		    updated_by_user_id = #{actorUserId},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	int update(
		@Param("id") UUID id,
		@Param("content") String content,
		@Param("sortOrder") Integer sortOrder,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);

	/**
	 * item의 sortOrder만 갱신한다. reorder용.
	 *
	 * @param id item 식별자
	 * @param sortOrder 새 sortOrder
	 * @param actorUserId 수정자
	 * @param now 수정 시각
	 * @return 영향받은 row 수. 0이면 대상이 없거나 이미 삭제됨
	 */
	@Update("""
		UPDATE planning.checklist_items
		SET sort_order = #{sortOrder},
		    updated_by_user_id = #{actorUserId},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	int updateSortOrder(
		@Param("id") UUID id,
		@Param("sortOrder") int sortOrder,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);

	/**
	 * item을 soft delete한다.
	 *
	 * @param id item 식별자
	 * @param actorUserId 삭제자
	 * @param now 삭제 시각
	 * @return 영향받은 row 수. 0이면 대상이 없거나 이미 삭제됨
	 */
	@Update("""
		UPDATE planning.checklist_items
		SET deleted_at = #{now},
		    deleted_by_user_id = #{actorUserId},
		    updated_by_user_id = #{actorUserId},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	int softDelete(
		@Param("id") UUID id,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);

	/**
	 * checklist의 모든 활성 item을 soft delete한다. checklist 삭제 cascade용.
	 *
	 * @param checklistId checklist 식별자
	 * @param actorUserId 삭제자
	 * @param now 삭제 시각
	 */
	@Update("""
		UPDATE planning.checklist_items
		SET deleted_at = #{now},
		    deleted_by_user_id = #{actorUserId},
		    updated_by_user_id = #{actorUserId},
		    updated_at = #{now}
		WHERE checklist_id = #{checklistId} AND deleted_at IS NULL
		""")
	void softDeleteByChecklistId(
		@Param("checklistId") UUID checklistId,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);
}
