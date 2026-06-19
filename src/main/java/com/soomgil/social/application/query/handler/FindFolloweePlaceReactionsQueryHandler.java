package com.soomgil.social.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.social.application.query.dto.FindFolloweePlaceReactionsQuery;
import com.soomgil.social.application.query.dto.FolloweePlaceReaction;
import java.util.List;

/**
 * 팔로우한 사용자의 장소별 긍정 반응을 조회하는 application 계약.
 */
public interface FindFolloweePlaceReactionsQueryHandler extends
	QueryHandler<FindFolloweePlaceReactionsQuery, List<FolloweePlaceReaction>> {
}
