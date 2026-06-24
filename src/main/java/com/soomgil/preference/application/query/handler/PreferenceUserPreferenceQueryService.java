package com.soomgil.preference.application.query.handler;

import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.preference.api.dto.MyPreferenceCategory;
import com.soomgil.preference.api.dto.MyPreferenceSummary;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceUserPreferenceMapper;
import com.soomgil.preference.infrastructure.persistence.row.UserPreferenceTagScoreRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 사용자의 여행 취향 분석 결과를 조립한다.
 *
 * <p>태그별 {@code preference_score}를 그룹 단위로 집계해 마이페이지 취향 패널 데이터를 만든다.
 */
@Service
public class PreferenceUserPreferenceQueryService {

	private static final int MAX_CATEGORIES = 5;
	private static final int MAX_TAGS = 8;
	private static final String EMPTY_TRAVEL_STYLE =
		"아직 학습된 취향이 없어요. 스와이프로 취향을 알려주세요.";

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final PreferenceUserPreferenceMapper mapper;

	public PreferenceUserPreferenceQueryService(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		PreferenceUserPreferenceMapper mapper
	) {
		this.currentUserProvider = currentUserProvider;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public MyPreferenceSummary listMyPreferences() {
		String userId = currentUserId().toString();
		if (!mapper.hasAnyReaction(userId)) {
			return new MyPreferenceSummary(List.of(), EMPTY_TRAVEL_STYLE, List.of());
		}

		List<UserPreferenceTagScoreRow> rows = mapper.findUserPreferenceScores(userId);
		if (rows.isEmpty()) {
			return new MyPreferenceSummary(List.of(), EMPTY_TRAVEL_STYLE, List.of());
		}

		return new MyPreferenceSummary(
			aggregateTopCategories(rows),
			buildTravelStyle(rows),
			collectPreferredTags(rows)
		);
	}

	private List<MyPreferenceCategory> aggregateTopCategories(List<UserPreferenceTagScoreRow> rows) {
		Map<String, GroupAccumulator> byGroup = new LinkedHashMap<>();
		for (UserPreferenceTagScoreRow row : rows) {
			GroupAccumulator acc = byGroup.computeIfAbsent(
				row.groupCode(),
				code -> new GroupAccumulator(code, row.groupDisplayName())
			);
			acc.add(row.preferenceScore());
		}
		return byGroup.values().stream()
			.map(GroupAccumulator::toCategory)
			.sorted(Comparator.comparingInt(MyPreferenceCategory::percentage).reversed())
			.limit(MAX_CATEGORIES)
			.toList();
	}

	private List<String> collectPreferredTags(List<UserPreferenceTagScoreRow> rows) {
		List<String> tags = new ArrayList<>();
		for (UserPreferenceTagScoreRow row : rows) {
			if (row.displayName() == null || row.displayName().isBlank()) {
				continue;
			}
			if (!tags.contains(row.displayName())) {
				tags.add(row.displayName());
			}
			if (tags.size() >= MAX_TAGS) {
				break;
			}
		}
		return tags;
	}

	private String buildTravelStyle(List<UserPreferenceTagScoreRow> rows) {
		List<MyPreferenceCategory> categories = aggregateTopCategories(rows);
		if (categories.isEmpty()) {
			return EMPTY_TRAVEL_STYLE;
		}
		if (categories.size() == 1) {
			return categories.get(0).category() + "을(를) 즐기는 여행을 선호해요.";
		}
		return categories.get(0).category() + "와 " + categories.get(1).category()
			+ "을(를) 함께 즐기는 여행을 선호해요.";
	}

	private UUID currentUserId() {
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		if (provider == null) {
			throw new IllegalStateException("CurrentUserProvider is required to read preferences.");
		}
		return provider.currentUserId();
	}

	private static final class GroupAccumulator {

		private final String groupCode;
		private final String displayName;
		private BigDecimal sum = BigDecimal.ZERO;
		private int count;

		GroupAccumulator(String groupCode, String displayName) {
			this.groupCode = groupCode;
			this.displayName = displayName;
		}

		void add(BigDecimal score) {
			if (score == null) {
				score = BigDecimal.ZERO;
			}
			sum = sum.add(score);
			count++;
		}

		MyPreferenceCategory toCategory() {
			BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
			int percentage = avg.multiply(BigDecimal.valueOf(100))
				.setScale(0, RoundingMode.HALF_UP)
				.intValue();
			return new MyPreferenceCategory(displayName, groupCode, percentage);
		}
	}
}
