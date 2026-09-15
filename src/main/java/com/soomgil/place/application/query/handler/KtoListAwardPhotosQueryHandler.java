package com.soomgil.place.application.query.handler;

import com.soomgil.place.api.dto.AwardPhoto;
import com.soomgil.place.application.port.AwardPhotoCatalogClient;
import com.soomgil.place.application.port.AwardPhotoCatalogItem;
import com.soomgil.place.application.query.dto.ListAwardPhotosQuery;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 관광공사 수상작 카탈로그를 화면용 DTO로 변환해 반환한다.
 *
 * <p>수상작 전체 규모가 100건 내외로 작아 페이징 대신 매일 회전하는 offset으로 노출 순서를 바꾼다.
 * 같은 날 같은 조건이면 항상 같은 결과를 반환하므로 화면 간 순서가 어긋나지 않는다.
 */
@Service
public class KtoListAwardPhotosQueryHandler implements ListAwardPhotosQueryHandler {

	private static final ZoneId KOREA_TIME = ZoneId.of("Asia/Seoul");
	private static final int DEFAULT_LIMIT = 10;
	private static final int MAX_LIMIT = 100;

	private final AwardPhotoCatalogClient catalogClient;
	private final Clock clock;

	@Autowired
	public KtoListAwardPhotosQueryHandler(AwardPhotoCatalogClient catalogClient) {
		this(catalogClient, Clock.system(KOREA_TIME));
	}

	public KtoListAwardPhotosQueryHandler(AwardPhotoCatalogClient catalogClient, Clock clock) {
		this.catalogClient = catalogClient;
		this.clock = clock;
	}

	@Override
	public List<AwardPhoto> handle(ListAwardPhotosQuery query) {
		List<AwardPhotoCatalogItem> catalog = catalogClient.fetchCatalog().stream()
			.filter(item -> matchesRegion(item, query.regionCode()))
			.toList();
		if (catalog.isEmpty()) {
			return List.of();
		}

		int limit = Math.min(normalizeLimit(query.limit()), catalog.size());
		int offset = Math.floorMod(LocalDate.now(clock).getDayOfYear(), catalog.size());
		List<AwardPhoto> selected = new ArrayList<>(limit);
		for (int index = 0; index < limit; index++) {
			selected.add(toAwardPhoto(catalog.get((offset + index) % catalog.size())));
		}
		return List.copyOf(selected);
	}

	private static boolean matchesRegion(AwardPhotoCatalogItem item, String regionCode) {
		if (regionCode == null || regionCode.isBlank()) {
			return true;
		}
		return regionCode.equals(item.regionCode());
	}

	private static int normalizeLimit(int limit) {
		if (limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private static AwardPhoto toAwardPhoto(AwardPhotoCatalogItem item) {
		return new AwardPhoto(
			item.awardContentId(),
			item.title(),
			placeNameOf(item.filmLocation()),
			regionNameOf(item.filmLocation()),
			item.filmLocation(),
			item.photographer(),
			item.awardDivision(),
			filmYearMonthOf(item.filmDay()),
			item.imageUrl(),
			item.thumbnailUrl(),
			item.copyrightCode(),
			item.regionCode()
		);
	}

	/**
	 * 촬영지 원문에서 관광지명을 추출한다.
	 *
	 * <p>{@code "경상남도 합천군, 가야산국립공원"}처럼 마지막 쉼표 뒤가 장소명이다.
	 * 쉼표가 없으면 원문 전체를 장소명으로 본다.
	 */
	private static String placeNameOf(String filmLocation) {
		if (filmLocation == null || filmLocation.isBlank()) {
			return null;
		}
		int separator = filmLocation.lastIndexOf(',');
		String placeName = separator < 0
			? filmLocation.trim()
			: filmLocation.substring(separator + 1).trim();
		return placeName.isEmpty() ? null : placeName;
	}

	private static String regionNameOf(String filmLocation) {
		if (filmLocation == null) {
			return null;
		}
		int separator = filmLocation.lastIndexOf(',');
		if (separator < 0) {
			return null;
		}
		String regionName = filmLocation.substring(0, separator).trim();
		return regionName.isEmpty() ? null : regionName;
	}

	/**
	 * {@code yyyyMM} 형식 촬영 시기를 {@code yyyy-MM}으로 변환한다. 형식이 맞지 않으면 null이다.
	 */
	private static String filmYearMonthOf(String filmDay) {
		if (filmDay == null || filmDay.length() != 6 || !filmDay.chars().allMatch(Character::isDigit)) {
			return null;
		}
		return filmDay.substring(0, 4) + "-" + filmDay.substring(4);
	}
}
