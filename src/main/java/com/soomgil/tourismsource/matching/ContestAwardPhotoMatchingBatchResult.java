package com.soomgil.tourismsource.matching;

/**
 * 공모전 수상작 사진 batch 매칭 결과.
 *
 * @param processedPhotos 처리한 사진 수
 * @param createdMatches 생성한 match row 수
 * @param selectedMatches 자동 확정한 match row 수
 */
public record ContestAwardPhotoMatchingBatchResult(
	int processedPhotos,
	int createdMatches,
	int selectedMatches
) {
}
