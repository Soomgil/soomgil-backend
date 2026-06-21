package com.soomgil.tourismsource.matching;

import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.tourismsource.matching.infrastructure.TourismSourceAttractionMatchRow;
import com.soomgil.tourismsource.matching.infrastructure.TourismSourceContestAwardPhotoMatchInsertRow;
import com.soomgil.tourismsource.matching.infrastructure.TourismSourceContestAwardPhotoMatchingMapper;
import com.soomgil.tourismsource.matching.infrastructure.TourismSourceContestAwardPhotoRow;
import com.soomgil.tourismsource.matching.infrastructure.TourismSourceRegionAliasRow;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관광 원천 수상작 사진 metadata로 관광지/지역 매칭 후보를 생성한다.
 */
@Service
public class TourismSourceContestAwardPhotoMatchingCommandHandler
	implements ContestAwardPhotoMatchingCommandHandler {

	private static final BigDecimal REGION_CONFIDENCE = new BigDecimal("0.7000");
	private static final BigDecimal ATTRACTION_CONFIDENCE = new BigDecimal("0.9500");
	private static final BigDecimal AUTO_SELECT_THRESHOLD = new BigDecimal("0.9000");

	private final TourismSourceContestAwardPhotoMatchingMapper mapper;

	public TourismSourceContestAwardPhotoMatchingCommandHandler(TourismSourceContestAwardPhotoMatchingMapper mapper) {
		this.mapper = mapper;
	}

	@Transactional
	@Override
	public ContestAwardPhotoMatchingResult handle(ContestAwardPhotoMatchingCommand command) {
		String photoId = command.photoId().toString();
		TourismSourceContestAwardPhotoRow photo = mapper.findPhoto(photoId);
		if (photo == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Contest award photo was not found.");
		}

		mapper.deleteGeneratedMatches(photoId);
		List<ContestAwardPhotoMatchCandidate> candidates = createCandidates(photo);
		boolean canAutoSelect = canAutoSelect(photoId, candidates);

		int selectedMatches = 0;
		for (ContestAwardPhotoMatchCandidate candidate : candidates) {
			boolean selected = canAutoSelect;
			if (selected) {
				selectedMatches++;
			}
			mapper.insertMatch(toInsertRow(command, candidate, status(candidate, candidates, selected), selected));
		}

		return new ContestAwardPhotoMatchingResult(command.photoId(), candidates.size(), selectedMatches);
	}

	private List<ContestAwardPhotoMatchCandidate> createCandidates(TourismSourceContestAwardPhotoRow photo) {
		String searchableText = normalize(String.join(
			" ",
			nullToEmpty(photo.title()),
			nullToEmpty(photo.originalFileName()),
			nullToEmpty(photo.extractedRegionText())
		));

		List<ContestAwardPhotoMatchCandidate> candidates = new ArrayList<>();
		for (TourismSourceRegionAliasRow alias : mapper.findActiveRegionAliases()) {
			if (containsNormalized(searchableText, alias.normalizedAlias())) {
				candidates.add(new ContestAwardPhotoMatchCandidate(
					null,
					alias.sidoCode(),
					alias.gugunCode(),
					"REGION",
					"FILE_NAME_REGION",
					REGION_CONFIDENCE
				));
			}
		}

		for (TourismSourceAttractionMatchRow attraction : mapper.findAttractions()) {
			if (containsNormalized(searchableText, attraction.title())) {
				candidates.add(new ContestAwardPhotoMatchCandidate(
					attraction.no(),
					null,
					null,
					"ATTRACTION",
					"TITLE_TEXT",
					ATTRACTION_CONFIDENCE
				));
			}
		}

		if (candidates.isEmpty()) {
			candidates.add(new ContestAwardPhotoMatchCandidate(
				null,
				null,
				null,
				"UNMATCHED",
				"IMPORT_METADATA",
				BigDecimal.ZERO.setScale(4)
			));
		}
		return candidates;
	}

	private boolean canAutoSelect(String photoId, List<ContestAwardPhotoMatchCandidate> candidates) {
		if (candidates.size() != 1 || mapper.existsSelectedMatch(photoId)) {
			return false;
		}

		ContestAwardPhotoMatchCandidate candidate = candidates.getFirst();
		return "ATTRACTION".equals(candidate.matchScope())
			&& candidate.confidence().compareTo(AUTO_SELECT_THRESHOLD) >= 0;
	}

	private String status(
		ContestAwardPhotoMatchCandidate candidate,
		List<ContestAwardPhotoMatchCandidate> candidates,
		boolean selected
	) {
		if (selected) {
			return "SELECTED";
		}
		if (!"UNMATCHED".equals(candidate.matchScope()) && candidates.size() > 1) {
			return "AMBIGUOUS";
		}
		return "CANDIDATE";
	}

	private TourismSourceContestAwardPhotoMatchInsertRow toInsertRow(
		ContestAwardPhotoMatchingCommand command,
		ContestAwardPhotoMatchCandidate candidate,
		String status,
		boolean selected
	) {
		return new TourismSourceContestAwardPhotoMatchInsertRow(
			Ids.newUuid().toString(),
			command.photoId().toString(),
			candidate.attractionNo(),
			candidate.sidoCode(),
			candidate.gugunCode(),
			candidate.matchScope(),
			status,
			candidate.matchMethod(),
			candidate.confidence(),
			selected,
			rationale(candidate)
		);
	}

	private String rationale(ContestAwardPhotoMatchCandidate candidate) {
		if ("ATTRACTION".equals(candidate.matchScope())) {
			return "Photo title or file name strongly matched an attraction title.";
		}
		if ("REGION".equals(candidate.matchScope())) {
			return "Photo title, file name, or metadata matched a region alias.";
		}
		return "No attraction title or region alias matched the photo metadata.";
	}

	private boolean containsNormalized(String searchableText, String value) {
		String normalizedValue = normalize(value);
		return !normalizedValue.isBlank() && searchableText.contains(normalizedValue);
	}

	private String normalize(String value) {
		if (value == null) {
			return "";
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase();
		return normalized.replaceAll("[^a-z0-9가-힣]", "");
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
