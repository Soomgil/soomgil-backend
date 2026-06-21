package com.soomgil.place.infrastructure.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceFeedRequest;
import com.soomgil.place.application.port.TourismPlaceFeedResult;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 한국관광공사 KorService2 API client.
 */
@Component
public class KtoTourismPlaceClient implements TourismPlaceFeedClient {

	private static final Map<String, String> CONTENT_TYPE_NAMES = Map.of(
		"12", "관광지",
		"14", "문화시설",
		"15", "축제·공연·행사",
		"25", "여행코스",
		"28", "레포츠",
		"32", "숙박",
		"38", "쇼핑",
		"39", "음식점"
	);

	private final KtoTourismPlaceProperties properties;
	private final RestClient restClient;

	public KtoTourismPlaceClient(KtoTourismPlaceProperties properties) {
		this.properties = properties;
		this.restClient = RestClient.builder().build();
	}

	@Override
	public TourismPlaceFeedResult fetch(TourismPlaceFeedRequest request) {
		validateConfiguration();
		String currentSeed = request.seed() == null || request.seed().isBlank()
			? UUID.randomUUID().toString()
			: request.seed();
		int page = Math.floorMod(currentSeed.hashCode(), 20) + 1;
		JsonNode listResponse = get(buildListUri(request, page));
		List<TourismPlaceFeedItem> places = parseList(listResponse).stream()
			.map(this::loadDetailSafely)
			.toList();
		return new TourismPlaceFeedResult(places, UUID.randomUUID().toString());
	}

	private TourismPlaceFeedItem loadDetailSafely(TourismPlaceFeedItem place) {
		try {
			return withDetail(place, get(buildDetailUri(place.externalPlaceId())));
		}
		catch (KtoTourismPlaceException exception) {
			return place;
		}
	}

	private JsonNode get(URI uri) {
		try {
			JsonNode response = restClient.get().uri(uri).retrieve().body(JsonNode.class);
			if (response == null) {
				throw new KtoTourismPlaceException("KTO response is empty.");
			}
			return response;
		}
		catch (RestClientException exception) {
			throw new KtoTourismPlaceException("KTO request failed.", exception);
		}
	}

	private URI buildListUri(TourismPlaceFeedRequest request, int page) {
		UriComponentsBuilder builder = commonUri("/areaBasedList2")
			.queryParam("numOfRows", Math.min(Math.max(request.limit(), 1), 50))
			.queryParam("pageNo", page)
			.queryParam("arrange", "Q");
		if (request.legalRegionCode() != null && !request.legalRegionCode().isBlank()) {
			builder.queryParam("areaCode", request.legalRegionCode());
		}
		if (request.category() != null && !request.category().isBlank()) {
			builder.queryParam("contentTypeId", request.category());
		}
		return builder.build(true).toUri();
	}

	private URI buildDetailUri(String contentId) {
		return commonUri("/detailCommon2")
			.queryParam("contentId", contentId)
			.build(true)
			.toUri();
	}

	private UriComponentsBuilder commonUri(String path) {
		return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
			.path(path)
			.queryParam("serviceKey", properties.getApiKey())
			.queryParam("MobileOS", "ETC")
			.queryParam("MobileApp", "Soomgil")
			.queryParam("_type", "json");
	}

	private void validateConfiguration() {
		if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
			throw new KtoTourismPlaceException("KTO API key is not configured.");
		}
	}

	static List<TourismPlaceFeedItem> parseList(JsonNode response) {
		JsonNode body = successfulBody(response);
		JsonNode items = body.path("items").path("item");
		if (!items.isArray()) {
			return List.of();
		}
		List<TourismPlaceFeedItem> result = new ArrayList<>();
		for (JsonNode item : items) {
			String contentId = text(item, "contentid");
			String title = text(item, "title");
			if (contentId == null || title == null) {
				continue;
			}
			List<String> photos = distinctUrls(text(item, "firstimage"), text(item, "firstimage2"));
			String contentType = text(item, "contenttypeid");
			result.add(new TourismPlaceFeedItem(
				contentId,
				title,
				joinAddress(text(item, "addr1"), text(item, "addr2")),
				number(item, "mapy"),
				number(item, "mapx"),
				photos.isEmpty() ? null : photos.getFirst(),
				CONTENT_TYPE_NAMES.getOrDefault(contentType, contentType),
				null,
				photos,
				modifiedAt(text(item, "modifiedtime"))
			));
		}
		return result;
	}

	static TourismPlaceFeedItem withDetail(TourismPlaceFeedItem place, JsonNode response) {
		JsonNode body = successfulBody(response);
		JsonNode items = body.path("items").path("item");
		if (!items.isArray() || items.isEmpty()) {
			return place;
		}
		JsonNode detail = items.get(0);
		List<String> photos = distinctUrls(
			place.photos().toArray(String[]::new),
			text(detail, "firstimage"),
			text(detail, "firstimage2")
		);
		return new TourismPlaceFeedItem(
			place.externalPlaceId(),
			place.name(),
			place.address(),
			place.lat(),
			place.lng(),
			place.thumbnailUrl(),
			place.category(),
			plainText(text(detail, "overview")),
			photos,
			newer(place.sourceModifiedAt(), modifiedAt(text(detail, "modifiedtime")))
		);
	}

	private static OffsetDateTime modifiedAt(String value) {
		if (value == null || value.length() != 14) {
			return null;
		}
		try {
			return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
				.atOffset(ZoneOffset.ofHours(9));
		}
		catch (java.time.format.DateTimeParseException exception) {
			return null;
		}
	}

	private static OffsetDateTime newer(OffsetDateTime first, OffsetDateTime second) {
		if (first == null) return second;
		if (second == null) return first;
		return first.isAfter(second) ? first : second;
	}

	private static JsonNode successfulBody(JsonNode response) {
		if (response.hasNonNull("resultCode") && !"0000".equals(response.path("resultCode").asText())) {
			throw new KtoTourismPlaceException(response.path("resultMsg").asText("KTO request failed."));
		}
		JsonNode root = response.path("response");
		JsonNode header = root.path("header");
		String code = header.path("resultCode").asText();
		if (!"0000".equals(code)) {
			throw new KtoTourismPlaceException(header.path("resultMsg").asText("KTO request failed."));
		}
		return root.path("body");
	}

	private static String plainText(String value) {
		if (value == null) {
			return null;
		}
		return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]+>", " "))
			.replaceAll("\\s+", " ")
			.trim();
	}

	private static String joinAddress(String first, String second) {
		return java.util.stream.Stream.of(first, second)
			.filter(value -> value != null && !value.isBlank())
			.reduce((left, right) -> left + " " + right)
			.orElse(null);
	}

	private static String text(JsonNode node, String field) {
		String value = node.path(field).asText("").trim();
		return value.isEmpty() ? null : value;
	}

	private static Double number(JsonNode node, String field) {
		String value = text(node, field);
		return value == null ? null : Double.valueOf(value);
	}

	private static List<String> distinctUrls(String... values) {
		LinkedHashSet<String> urls = new LinkedHashSet<>();
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				urls.add(value);
			}
		}
		return List.copyOf(urls);
	}

	private static List<String> distinctUrls(String[] existing, String... values) {
		List<String> all = new ArrayList<>(java.util.Arrays.asList(existing));
		all.addAll(java.util.Arrays.asList(values));
		return distinctUrls(all.toArray(String[]::new));
	}
}
