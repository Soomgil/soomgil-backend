package com.soomgil.search.api;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.search.api.dto.UnifiedSearchResponse;
import com.soomgil.search.application.UnifiedSearchQuery;
import com.soomgil.search.application.UnifiedSearchQueryHandler;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통합 검색 REST API.
 *
 * <p>4개 도메인(trip/place/community/user)을 한 번에 검색한다.
 * 자세히 보기 페이지는 각 도메인의 기존 검색 엔드포인트로 페이지네이션한다:
 * <ul>
 *   <li>trips: GET /api/v1/trips/my?q= (확장 필요)</li>
 *   <li>places: GET /api/v1/places/search?q=</li>
 *   <li>posts: GET /api/v1/community/posts?query= (sort=likes 확장 필요)</li>
 *   <li>users: GET /api/v1/users?q=</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

	private final UnifiedSearchQueryHandler handler;

	public SearchController(UnifiedSearchQueryHandler handler) {
		this.handler = handler;
	}

	@GetMapping
	public UnifiedSearchResponse search(
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(name = "q", required = false) String query,
		@RequestParam(name = "size", defaultValue = "4") int size
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Login required for search.");
		}
		UUID userId = currentUser.userId();
		return handler.handle(new UnifiedSearchQuery(userId, query, size));
	}
}
