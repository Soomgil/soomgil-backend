package com.soomgil.preference.application.command.handler;

import com.soomgil.preference.application.command.dto.GenerateSyntheticPersonaSwipesCommand;
import com.soomgil.preference.application.command.dto.GenerateSyntheticPersonaSwipesResult;
import com.soomgil.preference.config.PreferencePolicyProperties;
import com.soomgil.preference.domain.policy.DefaultSyntheticPersonaCatalog;
import com.soomgil.preference.domain.policy.SyntheticPersonaCatalogValidator;
import com.soomgil.preference.domain.policy.SyntheticPersonaDefinition;
import com.soomgil.preference.domain.policy.SyntheticPersonaNoiseInput;
import com.soomgil.preference.domain.policy.SyntheticPersonaNoisePolicy;
import com.soomgil.preference.domain.policy.SyntheticPersonaPlaceScore;
import com.soomgil.preference.domain.policy.SyntheticPersonaPlaceScoreCalculator;
import com.soomgil.preference.domain.policy.SyntheticPersonaSwipeGenerator;
import com.soomgil.preference.domain.policy.SyntheticPersonaSwipeInput;
import com.soomgil.preference.domain.policy.SyntheticPlaceTagInput;
import com.soomgil.preference.domain.policy.SyntheticSwipeReaction;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSyntheticPersonaMapper;
import com.soomgil.preference.infrastructure.persistence.row.SyntheticPersonaInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.SyntheticPersonaTagPreferenceInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.SyntheticPlaceTagSourceRow;
import com.soomgil.preference.infrastructure.persistence.row.SyntheticSwipeEventInsertRow;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 고정 50명 catalog와 최신 장소 태그로 결정적 합성 스와이프를 생성한다.
 *
 * <p>실제 사용자 이벤트에는 쓰지 않으며 {@code synthetic_swipe_events}에만 upsert한다.
 * 같은 generator version, persona, 장소, seed 조합은 한 행만 유지한다.
 */
@Service
public class PreferenceGenerateSyntheticPersonaSwipesHandler
	implements GenerateSyntheticPersonaSwipesHandler {

	private static final int MAX_PLACE_LIMIT = 100;
	private static final int EVENT_BATCH_SIZE = 500;

	private final PreferenceSyntheticPersonaMapper mapper;
	private final DefaultSyntheticPersonaCatalog catalog = new DefaultSyntheticPersonaCatalog();
	private final SyntheticPersonaCatalogValidator catalogValidator;
	private final SyntheticPersonaPlaceScoreCalculator scoreCalculator;
	private final SyntheticPersonaNoisePolicy noisePolicy;
	private final SyntheticPersonaSwipeGenerator swipeGenerator;
	private final BigDecimal hardStrength;
	private final BigDecimal softStrength;

	public PreferenceGenerateSyntheticPersonaSwipesHandler(
		PreferenceSyntheticPersonaMapper mapper,
		PreferencePolicyProperties properties
	) {
		this.mapper = mapper;
		PreferencePolicyProperties.SyntheticPersona policy = properties.getSyntheticPersona();
		this.catalogValidator = new SyntheticPersonaCatalogValidator(
			policy.getRequiredCount(),
			policy.getMaximumNoiseRate()
		);
		this.hardStrength = policy.getHardPreferenceStrength();
		this.softStrength = policy.getSoftPreferenceStrength();
		this.scoreCalculator = new SyntheticPersonaPlaceScoreCalculator(hardStrength, softStrength);
		this.noisePolicy = new SyntheticPersonaNoisePolicy(policy.getMaximumNoiseRate());
		this.swipeGenerator = new SyntheticPersonaSwipeGenerator(
			policy.getSuperLikeThreshold(),
			policy.getLikeThreshold(),
			policy.getNopeThreshold()
		);
	}

	@Override
	@Transactional
	public GenerateSyntheticPersonaSwipesResult handle(GenerateSyntheticPersonaSwipesCommand command) {
		validate(command);
		String generatorVersion = DefaultSyntheticPersonaCatalog.GENERATOR_VERSION;
		List<SyntheticPersonaDefinition> personas = catalog.personas();
		catalogValidator.validate(personas);

		Map<String, String> personaIds = personaIds(personas, generatorVersion);
		mapper.upsertPersonas(personaRows(personas, personaIds, generatorVersion));
		mapper.deleteTagPreferences(generatorVersion);
		List<SyntheticPersonaTagPreferenceInsertRow> preferenceRows = preferenceRows(personas, personaIds);
		int insertedPreferences = mapper.insertTagPreferences(preferenceRows);
		if (insertedPreferences != preferenceRows.size()) {
			throw new IllegalStateException("synthetic persona catalog contains a tag outside the active whitelist");
		}

		List<SyntheticPlaceTagSourceRow> sources = mapper.findPlaceTagSources(command.placeLimit());
		Map<PlaceKey, List<SyntheticPlaceTagSourceRow>> places = groupPlaces(sources);
		List<SyntheticSwipeEventInsertRow> events = generateEvents(
			personas,
			personaIds,
			places,
			generatorVersion
		);
		for (int start = 0; start < events.size(); start += EVENT_BATCH_SIZE) {
			mapper.upsertEvents(events.subList(start, Math.min(start + EVENT_BATCH_SIZE, events.size())));
		}

		return new GenerateSyntheticPersonaSwipesResult(
			generatorVersion,
			personas.size(),
			places.size(),
			events.size()
		);
	}

	private List<SyntheticSwipeEventInsertRow> generateEvents(
		List<SyntheticPersonaDefinition> personas,
		Map<String, String> personaIds,
		Map<PlaceKey, List<SyntheticPlaceTagSourceRow>> places,
		String generatorVersion
	) {
		List<SyntheticSwipeEventInsertRow> events = new ArrayList<>(personas.size() * places.size());
		for (SyntheticPersonaDefinition persona : personas) {
			for (Map.Entry<PlaceKey, List<SyntheticPlaceTagSourceRow>> place : places.entrySet()) {
				SyntheticPersonaPlaceScore placeScore = scoreCalculator.calculate(
					persona,
					place.getValue().stream()
						.map(row -> new SyntheticPlaceTagInput(row.tagCode(), row.confidence(), row.weight()))
						.toList()
				);
				BigDecimal effectiveScore = noisePolicy.apply(new SyntheticPersonaNoiseInput(
					persona.personaKey(),
					place.getKey().provider(),
					place.getKey().externalPlaceId(),
					persona.seed(),
					persona.noiseRate(),
					placeScore.score(),
					placeScore.hardLikeMatched() || placeScore.hardDislikeMatched()
				));
				SyntheticSwipeReaction reaction = swipeGenerator.generate(new SyntheticPersonaSwipeInput(
					persona.personaKey(),
					place.getKey().provider(),
					place.getKey().externalPlaceId(),
					persona.seed(),
					effectiveScore,
					placeScore.hardLikeMatched(),
					placeScore.hardDislikeMatched()
				));
				events.add(new SyntheticSwipeEventInsertRow(
					personaIds.get(persona.personaKey()),
					place.getKey().provider(),
					place.getKey().externalPlaceId(),
					reaction.name(),
					place.getKey().enrichmentId(),
					generatorVersion,
					persona.seed(),
					effectiveScore
				));
			}
		}
		return events;
	}

	private Map<PlaceKey, List<SyntheticPlaceTagSourceRow>> groupPlaces(
		List<SyntheticPlaceTagSourceRow> sources
	) {
		Map<PlaceKey, List<SyntheticPlaceTagSourceRow>> places = new LinkedHashMap<>();
		for (SyntheticPlaceTagSourceRow source : sources) {
			PlaceKey key = new PlaceKey(
				source.enrichmentId(),
				source.provider(),
				source.externalPlaceId()
			);
			places.computeIfAbsent(key, ignored -> new ArrayList<>()).add(source);
		}
		return places;
	}

	private Map<String, String> personaIds(
		List<SyntheticPersonaDefinition> personas,
		String generatorVersion
	) {
		Map<String, String> ids = new LinkedHashMap<>();
		for (SyntheticPersonaDefinition persona : personas) {
			ids.put(persona.personaKey(), UUID.nameUUIDFromBytes(
				(generatorVersion + ":" + persona.personaKey()).getBytes(StandardCharsets.UTF_8)
			).toString());
		}
		return ids;
	}

	private List<SyntheticPersonaInsertRow> personaRows(
		List<SyntheticPersonaDefinition> personas,
		Map<String, String> personaIds,
		String generatorVersion
	) {
		return personas.stream()
			.map(persona -> new SyntheticPersonaInsertRow(
				personaIds.get(persona.personaKey()),
				persona.personaKey(),
				persona.displayName(),
				persona.description(),
				generatorVersion,
				persona.seed(),
				persona.noiseRate()
			))
			.toList();
	}

	private List<SyntheticPersonaTagPreferenceInsertRow> preferenceRows(
		List<SyntheticPersonaDefinition> personas,
		Map<String, String> personaIds
	) {
		List<SyntheticPersonaTagPreferenceInsertRow> rows = new ArrayList<>();
		for (SyntheticPersonaDefinition persona : personas) {
			String personaId = personaIds.get(persona.personaKey());
			addPreferences(rows, personaId, persona.hardLikeTags(), "HARD_LIKE", hardStrength);
			addPreferences(rows, personaId, persona.hardDislikeTags(), "HARD_DISLIKE", hardStrength.negate());
			addPreferences(rows, personaId, persona.softLikeTags(), "SOFT_LIKE", softStrength);
			addPreferences(rows, personaId, persona.softDislikeTags(), "SOFT_DISLIKE", softStrength.negate());
			addPreferences(rows, personaId, persona.neutralTags(), "NEUTRAL", BigDecimal.ZERO);
		}
		return rows;
	}

	private void addPreferences(
		List<SyntheticPersonaTagPreferenceInsertRow> rows,
		String personaId,
		Set<String> tagCodes,
		String preferenceType,
		BigDecimal strength
	) {
		for (String tagCode : tagCodes) {
			rows.add(new SyntheticPersonaTagPreferenceInsertRow(
				personaId,
				tagCode,
				preferenceType,
				strength
			));
		}
	}

	private void validate(GenerateSyntheticPersonaSwipesCommand command) {
		if (command == null || command.placeLimit() < 1 || command.placeLimit() > MAX_PLACE_LIMIT) {
			throw new IllegalArgumentException("place limit must be between 1 and 100");
		}
	}

	private record PlaceKey(
		String enrichmentId,
		String provider,
		String externalPlaceId
	) {
	}
}
