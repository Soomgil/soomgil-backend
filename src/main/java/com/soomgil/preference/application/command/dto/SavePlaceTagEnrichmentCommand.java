package com.soomgil.preference.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 장소 태깅 실행 결과를 저장하는 command.
 *
 * <p>handler는 후보 전체를 보존하되, 고정 태그 사전에 없는 후보는 확정 태그로 저장하지 않는다.
 *
 * @param provider 장소 원천 provider
 * @param externalPlaceId provider가 부여한 외부 장소 id
 * @param sourceModifiedAt 원천 장소 수정 시각
 * @param sourceHash 태깅 입력 source hash
 * @param modelProvider 태깅에 사용한 모델 provider
 * @param modelName 태깅에 사용한 모델명
 * @param promptVersion prompt 버전
 * @param selectionPolicyVersion 후보 선택 정책 버전
 * @param candidates 저장할 후보 태그 목록
 */
public record SavePlaceTagEnrichmentCommand(
	String provider,
	String externalPlaceId,
	OffsetDateTime sourceModifiedAt,
	String sourceHash,
	String modelProvider,
	String modelName,
	String promptVersion,
	String selectionPolicyVersion,
	List<SavePlaceTagCandidateCommand> candidates
) implements Command<SavePlaceTagEnrichmentResult> {
}
