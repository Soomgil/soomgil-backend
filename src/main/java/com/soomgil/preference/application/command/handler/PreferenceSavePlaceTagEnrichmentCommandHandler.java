package com.soomgil.preference.application.command.handler;

import com.soomgil.common.id.Ids;
import com.soomgil.preference.application.command.dto.SavePlaceTagCandidateCommand;
import com.soomgil.preference.application.command.dto.SavePlaceTagEnrichmentCommand;
import com.soomgil.preference.application.command.dto.SavePlaceTagEnrichmentResult;
import com.soomgil.preference.config.PreferencePolicyProperties;
import com.soomgil.preference.domain.policy.PlaceTagSelectionDecision;
import com.soomgil.preference.domain.policy.PlaceTagSelectionInput;
import com.soomgil.preference.domain.policy.PlaceTagSelector;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferencePlaceTagEnrichmentMapper;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEnrichmentCandidateInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEnrichmentInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEnrichmentTagInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.PreferenceTagLookupRow;
import java.math.BigDecimal;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
	private final PlaceTagSelector selector;

	public PreferenceSavePlaceTagEnrichmentCommandHandler(
		PreferencePlaceTagEnrichmentMapper mapper,
		PreferencePolicyProperties properties
	) {
		this.mapper = mapper;
		this.selector = new PlaceTagSelector(
			properties.getTagSelection().getMinimumConfidence(),
			properties.getTagSelection().getMaximumConfirmedTags()
		);
	}

	@Transactional
	@Override
	public SavePlaceTagEnrichmentResult handle(SavePlaceTagEnrichmentCommand command) {
		UUID enrichmentId = Ids.newUuid();
		List<SavePlaceTagCandidateCommand> candidates = candidates(command);
		Map<String, PreferenceTagLookupRow> tagsByCode = findTagsByCode(candidates);
		Map<PlaceTagSelectionInput, SavePlaceTagCandidateCommand> candidatesByInput = new IdentityHashMap<>();
		List<PlaceTagSelectionInput> inputs = candidates.stream()
			.map(candidate -> selectionInput(candidate, tagsByCode, candidatesByInput))
			.toList();
		List<PlaceTagSelectionDecision> decisions = selector.select(inputs);
		int selectedCount = (int) decisions.stream()
			.filter(PlaceTagSelectionDecision::selected)
			.count();
		String tagStatisticRunId = decisions.stream()
			.filter(PlaceTagSelectionDecision::selected)
			.map(decision -> decision.input().tagStatisticRunId())
			.filter(runId -> runId != null && !runId.isBlank())
			.findFirst()
			.orElse(null);

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
			tagStatisticRunId,
			candidates.size(),
			selectedCount
		));

		int rankOrder = 1;
		for (PlaceTagSelectionDecision decision : decisions) {
			SavePlaceTagCandidateCommand candidate = candidatesByInput.get(decision.input());
			mapper.insertCandidate(new PlaceTagEnrichmentCandidateInsertRow(
				Ids.newUuid().toString(),
				enrichmentId.toString(),
				candidate.candidateCode(),
				decision.input().tagId(),
				candidate.confidence(),
				candidate.weight(),
				decision.selectionScore(),
				decision.status(),
				candidate.rationale()
			));

			if (decision.selected()) {
				mapper.insertSelectedTag(new PlaceTagEnrichmentTagInsertRow(
					enrichmentId.toString(),
					decision.input().tagId(),
					candidate.confidence(),
					candidate.weight(),
					decision.input().preferenceDiscrimination(),
					decision.selectionScore(),
					rankOrder,
					decision.input().tagStatisticRunId(),
					candidate.rationale()
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
		return mapper.findTagsByCodes(codes)
			.stream()
			.collect(Collectors.toMap(PreferenceTagLookupRow::code, Function.identity()));
	}

	private PlaceTagSelectionInput selectionInput(
		SavePlaceTagCandidateCommand candidate,
		Map<String, PreferenceTagLookupRow> tagsByCode,
		Map<PlaceTagSelectionInput, SavePlaceTagCandidateCommand> candidatesByInput
	) {
		PreferenceTagLookupRow tag = tagsByCode.get(candidate.candidateCode());
		PlaceTagSelectionInput input = new PlaceTagSelectionInput(
			tag == null ? null : tag.id(),
			candidate.candidateCode(),
			tag != null && Boolean.TRUE.equals(tag.activeSelectable()),
			candidate.confidence(),
			candidate.weight(),
			tag == null ? new BigDecimal("0.5") : tag.preferenceDiscrimination(),
			tag == null ? null : tag.tagStatisticRunId()
		);
		candidatesByInput.put(input, candidate);
		return input;
	}
}
