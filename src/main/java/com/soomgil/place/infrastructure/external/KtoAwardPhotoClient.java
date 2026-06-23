package com.soomgil.place.infrastructure.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * KTO 관광공모전 수상작 목록에서 장소와 명확히 일치하는 공개 이미지를 조회한다.
 *
 * <p>수상작 원본 URL은 KTO가 직접 제공하므로 별도 다운로드 없이 사용한다.
 */
@Component
public class KtoAwardPhotoClient {

	private static final Logger log = LoggerFactory.getLogger(KtoAwardPhotoClient.class);
	private static final String CACHE_NAME = "ktoAwardPhotoCatalog";
	private static final ZoneId KOREA_TIME = ZoneId.of("Asia/Seoul");

	private final KtoTourismPlaceProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;
	private final Cache cache;

	public KtoAwardPhotoClient(
		KtoTourismPlaceProperties properties,
		ObjectMapper objectMapper,
		CacheManager cacheManager
	) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(2))
			.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(6));
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
		this.cache = cacheManager.getCache(CACHE_NAME);
		if (this.cache == null) {
			throw new IllegalStateException("Missing cache: " + CACHE_NAME);
		}
	}

	/**
	 * 촬영지·제목·키워드에 장소명이 명확히 포함된 Type1 또는 Type3 수상작을 조회한다.
	 */
	public Optional<String> findBest(String placeName) {
		if (placeName == null || placeName.isBlank()) {
			return Optional.empty();
		}
		try {
			return findBestAwardPhoto(loadCatalog(), placeName);
		}
		catch (KtoTourismPlaceException exception) {
			log.warn("KTO award photo lookup failed for placeName={}", placeName, exception);
			return Optional.empty();
		}
	}

	private synchronized JsonNode loadCatalog() {
		String cacheKey = "catalog-v1:" + LocalDate.now(KOREA_TIME);
		String cached = cache.get(cacheKey, String.class);
		if (cached != null) {
			try {
				return objectMapper.readTree(cached);
			}
			catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
				cache.evict(cacheKey);
			}
		}

		validateConfiguration();
		try {
			JsonNode response = restClient.get().uri(buildCatalogUri()).retrieve().body(JsonNode.class);
			if (response == null) {
				throw new KtoTourismPlaceException("KTO award response is empty.");
			}
			cache.put(cacheKey, response.toString());
			return response;
		}
		catch (RestClientException exception) {
			throw new KtoTourismPlaceException("KTO award request failed.", exception);
		}
	}

	private URI buildCatalogUri() {
		return UriComponentsBuilder.fromUriString(properties.getAwardBaseUrl())
			.path("/phokoAwrdList")
			.queryParam("serviceKey", properties.getApiKey())
			.queryParam("MobileOS", "ETC")
			.queryParam("MobileApp", "Soomgil")
			.queryParam("_type", "json")
			.queryParam("numOfRows", 100)
			.queryParam("pageNo", 1)
			.build(true)
			.toUri();
	}

	private void validateConfiguration() {
		if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
			throw new KtoTourismPlaceException("KTO API key is not configured.");
		}
	}

	static Optional<String> findBestAwardPhoto(JsonNode response, String placeName) {
		JsonNode body = successfulBody(response);
		JsonNode items = body.path("items").path("item");
		if (!items.isArray()) {
			return Optional.empty();
		}
		String normalizedPlaceName = normalize(placeName);
		if (normalizedPlaceName.length() < 3) {
			return Optional.empty();
		}

		List<AwardCandidate> candidates = new ArrayList<>();
		for (JsonNode item : items) {
			String license = text(item, "cpyrhtDivCd");
			String image = text(item, "orgImage");
			if (!("Type1".equalsIgnoreCase(license) || "Type3".equalsIgnoreCase(license)) || image == null) {
				continue;
			}
			int matchScore = matchScore(item, normalizedPlaceName);
			if (matchScore > 0) {
				candidates.add(new AwardCandidate(image, license, matchScore));
			}
		}

		return candidates.stream()
			.sorted(Comparator.comparingInt(AwardCandidate::matchScore).reversed()
				.thenComparingInt(candidate -> "Type1".equalsIgnoreCase(candidate.license()) ? 0 : 1))
			.map(AwardCandidate::image)
			.findFirst();
	}

	private static int matchScore(JsonNode item, String placeName) {
		if (normalize(text(item, "koFilmst")).contains(placeName)) return 3;
		if (normalize(text(item, "koTitle")).contains(placeName)) return 2;
		if (normalize(text(item, "koKeyWord")).contains(placeName)) return 1;
		return 0;
	}

	private static JsonNode successfulBody(JsonNode response) {
		JsonNode header = response.path("response").path("header");
		if (!"0000".equals(header.path("resultCode").asText())) {
			throw new KtoTourismPlaceException(header.path("resultMsg").asText("KTO award request failed."));
		}
		return response.path("response").path("body");
	}

	private static String text(JsonNode node, String field) {
		String value = node.path(field).asText("").trim();
		return value.isEmpty() ? null : value;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^가-힣a-z0-9]", "");
	}

	private record AwardCandidate(String image, String license, int matchScore) {
	}
}
