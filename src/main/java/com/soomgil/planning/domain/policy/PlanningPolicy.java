package com.soomgil.planning.domain.policy;

import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.domain.model.PlanningException;
import java.util.UUID;

/**
 * planning 입력값/상태 검증 규칙.
 *
 * <p>DB 제약과 별개로 비즈니스 의미 단위 검증을 모아둔다.
 * Controller {@code @Valid} 어노테이션과互补적으로 handler에서 재검증한다.
 */
public final class PlanningPolicy {

	/** note 본문 최대 길이. */
	public static final int NOTE_CONTENT_MAX = 10_000;
	/** checklist title 최대 길이. */
	public static final int CHECKLIST_TITLE_MAX = 200;
	/** checklist item 본문 최대 길이. */
	public static final int ITEM_CONTENT_MAX = 500;
	/** sort_order 상한. 음수/과도한 값 방지용. */
	public static final int SORT_ORDER_MAX = 9_999;

	private PlanningPolicy() {
	}

	/**
	 * scope과 itinerary_day_id 조합이 일관적인지 검증한다.
	 *
	 * <p>규칙:
	 * <ul>
	 *   <li>{@code DAY} scope은 {@code itineraryDayId}가 필수</li>
	 *   <li>{@code TRIP} scope은 {@code itineraryDayId}가 null이어야 함</li>
	 * </ul>
	 * 위반 시 {@link ErrorCode#PLANNING_SCOPE_DAY_MISMATCH}를 던진다.
	 *
	 * @param scopeType scope
	 * @param itineraryDayId 일차 식별자 (nullable)
	 */
	public static void validateScopeDay(PlanningScopeType scopeType, UUID itineraryDayId) {
		if (scopeType == PlanningScopeType.DAY && itineraryDayId == null) {
			throw new PlanningException(ErrorCode.PLANNING_SCOPE_DAY_MISMATCH);
		}
		if (scopeType == PlanningScopeType.TRIP && itineraryDayId != null) {
			throw new PlanningException(ErrorCode.PLANNING_SCOPE_DAY_MISMATCH);
		}
	}

	/**
	 * note 본문 길이가 유효한지 검사.
	 *
	 * @param content 본문
	 * @return null이 아니고 {@value NOTE_CONTENT_MAX}자 이하면 true
	 */
	public static boolean isValidNoteContent(String content) {
		return content != null && content.length() <= NOTE_CONTENT_MAX;
	}

	/**
	 * checklist title 길이가 유효한지 검사.
	 *
	 * @param title 제목 (nullable)
	 * @return null이거나 {@value CHECKLIST_TITLE_MAX}자 이하면 true
	 */
	public static boolean isValidTitle(String title) {
		return title == null || title.length() <= CHECKLIST_TITLE_MAX;
	}

	/**
	 * checklist item 본문 길이가 유효한지 검사.
	 *
	 * @param content 본문
	 * @return null이 아니고 {@value ITEM_CONTENT_MAX}자 이하면 true
	 */
	public static boolean isValidItemContent(String content) {
		return content != null && content.length() <= ITEM_CONTENT_MAX;
	}

	/**
	 * sort_order 값이 유효한지 검사.
	 *
	 * @param sortOrder 정렬 순서
	 * @return 0 이상 {@value SORT_ORDER_MAX} 이하면 true
	 */
	public static boolean isValidSortOrder(Integer sortOrder) {
		return sortOrder != null && sortOrder >= 0 && sortOrder <= SORT_ORDER_MAX;
	}
}
