package com.soomgil.tourismsource.matching;

import com.soomgil.common.id.Ids;
import com.soomgil.tourismsource.matching.infrastructure.TourismSourceContestAwardPhotoMatchingMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 아직 매칭되지 않은 공모전 수상작 사진을 batch로 매칭한다.
 */
@Service
public class ContestAwardPhotoMatchingBatchService {

	private static final int DEFAULT_LIMIT = 100;
	private static final int MAX_LIMIT = 1000;

	private final TourismSourceContestAwardPhotoMatchingMapper mapper;
	private final ContestAwardPhotoMatchingCommandHandler handler;

	public ContestAwardPhotoMatchingBatchService(
		TourismSourceContestAwardPhotoMatchingMapper mapper,
		ContestAwardPhotoMatchingCommandHandler handler
	) {
		this.mapper = mapper;
		this.handler = handler;
	}

	/**
	 * 매칭 row가 아직 없는 수상작 사진을 최대 limit개 처리한다.
	 *
	 * @param limit 최대 처리 수
	 * @return batch 처리 결과
	 */
	public ContestAwardPhotoMatchingBatchResult runPending(int limit) {
		List<String> photoIds = mapper.findPendingPhotoIds(normalizeLimit(limit));
		int createdMatches = 0;
		int selectedMatches = 0;

		for (String photoId : photoIds) {
			ContestAwardPhotoMatchingResult result = handler.handle(new ContestAwardPhotoMatchingCommand(
				Ids.parseUuid(photoId, "photoId")
			));
			createdMatches += result.createdMatches();
			selectedMatches += result.selectedMatches();
		}

		return new ContestAwardPhotoMatchingBatchResult(photoIds.size(), createdMatches, selectedMatches);
	}

	private int normalizeLimit(int limit) {
		if (limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
