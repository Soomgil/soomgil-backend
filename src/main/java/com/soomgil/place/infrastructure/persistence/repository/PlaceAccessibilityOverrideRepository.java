package com.soomgil.place.infrastructure.persistence.repository;

import com.soomgil.place.application.query.dto.AccessibilityFlag;
import com.soomgil.place.application.query.dto.ParkingType;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceAccessibilityOverrideRepository {

	private final JdbcTemplate jdbcTemplate;

	public PlaceAccessibilityOverrideRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<PlaceAccessibilityInfo> find(String provider, String externalPlaceId) {
		if (provider == null || provider.isBlank() || externalPlaceId == null || externalPlaceId.isBlank()) {
			return Optional.empty();
		}
		return jdbcTemplate.query("""
				SELECT opening_hours, closed_days, parking_type, flags_csv, unavailable_flags_csv
				FROM tourism_source.place_accessibility_overrides
				WHERE provider = ? AND external_place_id = ?
				""",
			(resultSet, rowNumber) -> new PlaceAccessibilityInfo(
				resultSet.getString("opening_hours"),
				resultSet.getString("closed_days"),
				parseParkingType(resultSet.getString("parking_type")),
				parseFlags(resultSet.getString("flags_csv")),
				parseFlags(resultSet.getString("unavailable_flags_csv"))
			),
			provider,
			externalPlaceId
		).stream().findFirst();
	}

	private ParkingType parseParkingType(String value) {
		if (value == null || value.isBlank()) {
			return ParkingType.UNKNOWN;
		}
		try {
			return ParkingType.valueOf(value);
		}
		catch (IllegalArgumentException exception) {
			return ParkingType.UNKNOWN;
		}
	}

	private Set<AccessibilityFlag> parseFlags(String value) {
		if (value == null || value.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(value.split(","))
			.map(String::trim)
			.filter(flag -> !flag.isBlank())
			.map(this::parseFlag)
			.flatMap(Optional::stream)
			.collect(Collectors.toUnmodifiableSet());
	}

	private Optional<AccessibilityFlag> parseFlag(String value) {
		try {
			return Optional.of(AccessibilityFlag.valueOf(value));
		}
		catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}
}
