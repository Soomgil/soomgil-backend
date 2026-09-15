package com.soomgil.place.application.query.handler;

import com.soomgil.place.api.dto.AwardPhoto;
import com.soomgil.place.application.query.dto.ListAwardPhotosQuery;
import java.util.List;

/**
 * 관광사진 공모전 수상작 목록을 조회한다.
 */
public interface ListAwardPhotosQueryHandler {

	/**
	 * 수상작 목록을 조회한다.
	 *
	 * <p>노출 순서는 매일 한 번씩 회전해 같은 날에는 항상 같은 결과를 반환한다.
	 * 외부 API 조회에 실패하면 예외 대신 빈 목록을 반환한다.
	 *
	 * @param query 조회 조건
	 * @return 수상작 목록. 조회 실패 시 빈 목록
	 */
	List<AwardPhoto> handle(ListAwardPhotosQuery query);
}
