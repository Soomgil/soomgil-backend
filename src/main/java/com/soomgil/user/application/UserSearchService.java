package com.soomgil.user.application;

import com.soomgil.search.api.dto.UserSearchResult;
import com.soomgil.social.infrastructure.persistence.UserFollowMapper;
import com.soomgil.user.application.handler.SearchUsersQueryHandler;
import com.soomgil.user.application.query.SearchUsersQuery;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 검색용 사용자 검색 서비스.
 *
 * <p>기존 SearchUsersQueryHandler로 display_name 키워드 매칭 사용자를 찾고,
 * UserFollowMapper로 팔로워 수를 덧붙인 뒤 팔로워 수 내림차순 정렬한다.
 */
@Service
public class UserSearchService {

	private final SearchUsersQueryHandler searchHandler;
	private final UserFollowMapper userFollowMapper;

	public UserSearchService(SearchUsersQueryHandler searchHandler, UserFollowMapper userFollowMapper) {
		this.searchHandler = searchHandler;
		this.userFollowMapper = userFollowMapper;
	}

	@Transactional(readOnly = true)
	public List<UserSearchResult> searchWithFollowerCount(String q, int size) {
		var paged = searchHandler.handle(new SearchUsersQuery(q, 0, size));
		return paged.items().stream()
			.map(user -> new UserSearchResult(
				user.id(),
				user.displayName(),
				user.profileImageUrl(),
				userFollowMapper.countFollowers(user.id())
			))
			.sorted(Comparator.comparingLong(UserSearchResult::followerCount).reversed())
			.toList();
	}
}
