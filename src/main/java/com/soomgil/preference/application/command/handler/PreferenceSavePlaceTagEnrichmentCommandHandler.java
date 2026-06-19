package com.soomgil.preference.application.command.handler;

import com.soomgil.common.id.Ids;
import com.soomgil.preference.application.command.dto.SavePlaceTagCandidateCommand;
import com.soomgil.preference.application.command.dto.SavePlaceTagEnrichmentCommand;
import com.soomgil.preference.application.command.dto.SavePlaceTagEnrichmentResult;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferencePlaceTagEnrichmentMapper;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEnrichmentCandidateInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEnrichmentInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEnrichmentTagInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.PreferenceTagLookupRow;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장소 태깅 실행 결과를 preference 저장소에 기록한다.
 */
@Service
public class PreferenceSavePlaceTagEnrichmentCommandHandler implements SavePlaceTagEnrichmentCommandHandler {

	private static final String DICTIONARY_VERSION = "preference-tags-v1";

	private final PreferencePlaceTagEnrichmentMapper mapper;

	public PreferenceSavePlaceTagEnrichmentCommandHandler(PreferencePlaceTagEnrichmentMapper mapper) {
		this.mapper = mapper;
	}

	@Transactional
	@Override
	public SavePlaceTagEnrichmentResult handle(SavePlaceTagEnrichmentCommand command) {
		UUID enrichmentId = Ids.newUuid();
		List<SavePlaceTagCandidateCommand> candidates = candidates(command);
		Map<String, PreferenceTagLookupRow> tagsByCode = findTagsByCode(candidates);
		List<CandidateDecision> decisions = decideCandidates(candidates, tagsByCode);
		int selectedCount = (int) decisions.stream()
			.filter(CandidateDecision::selected)
			.count();

		mapper.insertEnrichment(new PlaceTagEnrichmentInsertRow(
			enrichmentId.toString(),
			command.provider(),
			command.externalPlaceId(),
			command.sourceModifiedAt(),
			command.sourceHash(),
			"SUCCEEDED",
			command.modelProvider(),
			command.modelName(),
			command.promptVersion(),
			DICTIONARY_VERSION,
			command.selectionPolicyVersion(),
			candidates.size(),
			selectedCount
		));

		int rankOrder = 1;
		for (CandidateDecision decision : decisions) {
			mapper.insertCandidate(new PlaceTagEnrichmentCandidateInsertRow(
				Ids.newUuid().toString(),
				enrichmentId.toString(),
				decision.candidate().candidateCode(),
				decision.matchedTagId(),
				decision.candidate().confidence(),
				decision.candidate().weight(),
				null,
				decision.status(),
				decision.candidate().rationale()
			));

			if (decision.selected()) {
				mapper.insertSelectedTag(new PlaceTagEnrichmentTagInsertRow(
					enrichmentId.toString(),
					decision.matchedTagId(),
					decision.candidate().confidence(),
					decision.candidate().weight(),
					null,
					null,
					rankOrder,
					null,
					decision.candidate().rationale()
				));
				rankOrder++;
			}
		}

		return new SavePlaceTagEnrichmentResult(enrichmentId, candidates.size(), selectedCount);
	}

	private List<SavePlaceTagCandidateCommand> candidates(SavePlaceTagEnrichmentCommand command) {
		if (command.candidates() == null) {
			return List.of();
		}
		return command.candidates();
	}

	private Map<String, PreferenceTagLookupRow> findTagsByCode(List<SavePlaceTagCandidateCommand> candidates) {
		List<String> codes = candidates.stream()
			.map(SavePlaceTagCandidateCommand::candidateCode)
			.distinct()
			.toList();
		if (codes.isEmpty()) {
			return Map.of();
		}
		return mapper.findActiveSelectableTags(codes)
			.stream()
			.collect(Collectors.toMap(PreferenceTagLookupRow::code, Function.identity()));
	}

	private List<CandidateDecision> decideCandidates(
		List<SavePlaceTagCandidateCommand> candidates,
		Map<String, PreferenceTagLookupRow> tagsByCode
	) {
		Set<String> selectedTagIds = new HashSet<>();
		return candidates.stream()
			.map(candidate -> decideCandidate(candidate, tagsByCode, selectedTagIds))
			.toList();
	}

	private CandidateDecision decideCandidate(
		SavePlaceTagCandidateCommand candidate,
		Map<String, PreferenceTagLookupRow> tagsByCode,
		Set<String> selectedTagIds
	) {
		PreferenceTagLookupRow tag = tagsByCode.get(candidate.candidateCode());
		if (tag == null) {
			return new CandidateDecision(candidate, null, "REJECTED_OUT_OF_DICTIONARY");
		}
		if (!candidate.selected()) {
			return new CandidateDecision(candidate, tag.id(), "REJECTED_LOW_SCORE");
		}
		if (!selectedTagIds.add(tag.id())) {
			return new CandidateDecision(candidate, tag.id(), "REJECTED_DUPLICATE");
		}
		return new CandidateDecision(candidate, tag.id(), "SELECTED");
	}

	private record CandidateDecision(
		SavePlaceTagCandidateCommand candidate,
		String matchedTagId,
		String status
	) {

		private boolean selected() {
			return "SELECTED".equals(status);
		}
	}
}
