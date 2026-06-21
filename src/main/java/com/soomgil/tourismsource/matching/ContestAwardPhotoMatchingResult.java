package com.soomgil.tourismsource.matching;

import java.util.UUID;

/**
 * 공모전 수상작 사진 매칭 생성 결과.
 *
 * @param photoId 수상작 사진 id
 * @param createdMatches 생성한 match row 수
 * @param selectedMatches 자동 확정한 match row 수
 */
public record ContestAwardPhotoMatchingResult(
	UUID photoId,
	int createdMatches,
	int selectedMatches
) {
}
