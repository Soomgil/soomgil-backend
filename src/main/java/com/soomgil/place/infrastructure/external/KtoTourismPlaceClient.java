package com.soomgil.place.infrastructure.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.soomgil.place.application.port.PlaceIntroRaw;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceFeedRequest;
import com.soomgil.place.application.port.TourismPlaceFeedResult;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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

	private static final Logger log = LoggerFactory.getLogger(KtoTourismPlaceClient.class);
	private static final int DETAIL_CONCURRENCY = 12;
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(4);

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
	private final KtoPlaceDescriptionCache descriptionCache;
	private final KtoPlacePhotoCache photoCache;
	private final KtoAwardPhotoClient awardPhotoClient;
	private final RestClient restClient;
	private final ExecutorService detailExecutor;

	public KtoTourismPlaceClient(
		KtoTourismPlaceProperties properties,
		KtoPlaceDescriptionCache descriptionCache,
		KtoPlacePhotoCache photoCache,
		KtoAwardPhotoClient awardPhotoClient
	) {
		this.properties = properties;
		this.descriptionCache = descriptionCache;
		this.photoCache = photoCache;
		this.awardPhotoClient = awardPhotoClient;
		HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(READ_TIMEOUT);
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
		this.detailExecutor = Executors.newFixedThreadPool(
			DETAIL_CONCURRENCY,
			Thread.ofPlatform().daemon(true).name("kto-detail-", 0).factory()
		);
	}

	@Override
	public TourismPlaceFeedResult fetch(TourismPlaceFeedRequest request) {
		validateConfiguration();
		String currentSeed = request.seed() == null || request.seed().isBlank()
			? UUID.randomUUID().toString()
			: request.seed();
		int page = Math.floorMod(currentSeed.hashCode(), 20) + 1;
		JsonNode listResponse = get(buildListUri(request, page));
		List<TourismPlaceFeedItem> places = enrichDetailsConcurrently(
			parseList(listResponse),
			this::loadDetailSafely,
			detailExecutor
		);
		return new TourismPlaceFeedResult(places, UUID.randomUUID().toString());
	}

	@Override
	public PlaceIntroRaw fetchIntro(String contentId, String contentTypeId) {
		validateConfiguration();
		if (contentId == null || contentId.isBlank()) {
			return PlaceIntroRaw.empty();
		}
		CompletableFuture<PlaceIntroRaw> introTask = CompletableFuture.supplyAsync(
			() -> fetchIntroSafely(contentId, contentTypeId),
			detailExecutor
		);
		CompletableFuture<PlaceIntroRaw> barrierFreeTask = CompletableFuture.supplyAsync(
			() -> fetchBarrierFreeSafely(contentId),
			detailExecutor
		);
		return mergeIntro(introTask.join(), barrierFreeTask.join());
	}

	private PlaceIntroRaw fetchIntroSafely(String contentId, String contentTypeId) {
		try {
			return parseIntro(get(buildIntroUri(contentId, contentTypeId)), contentTypeId);
		}
		catch (KtoTourismPlaceException exception) {
			return PlaceIntroRaw.empty();
		}
	}

	private PlaceIntroRaw fetchBarrierFreeSafely(String contentId) {
		try {
			return parseBarrierFree(get(buildBarrierFreeUri(contentId)));
		}
		catch (KtoTourismPlaceException exception) {
			return PlaceIntroRaw.empty();
		}
	}

	static List<TourismPlaceFeedItem> enrichDetailsConcurrently(
		List<TourismPlaceFeedItem> places,
		Function<TourismPlaceFeedItem, TourismPlaceFeedItem> detailLoader,
		Executor executor
	) {
		List<CompletableFuture<TourismPlaceFeedItem>> tasks = places.stream()
			.map(place -> CompletableFuture.supplyAsync(() -> detailLoader.apply(place), executor))
			.toList();
		return tasks.stream().map(CompletableFuture::join).toList();
	}

	@PreDestroy
	void close() {
		detailExecutor.shutdownNow();
	}

	private TourismPlaceFeedItem loadDetailSafely(TourismPlaceFeedItem place) {
		TourismPlaceFeedItem described = loadDescriptionSafely(place);
		List<String> detailPhotos = loadPhotosSafely(place);
		String awardPhoto = awardPhotoClient.findBest(place.name()).orElse(null);
		return withPhotos(described, awardPhoto, detailPhotos);
	}

	private TourismPlaceFeedItem loadDescriptionSafely(TourismPlaceFeedItem place) {
		var cachedDescription = descriptionCache.find(place);
		if (cachedDescription.isPresent()) {
			return withDescription(place, cachedDescription.get());
		}
		try {
			TourismPlaceFeedItem enriched = withDetail(place, get(buildDetailUri(place.externalPlaceId())));
			descriptionCache.put(place, enriched.description());
			return enriched;
		}
		catch (KtoTourismPlaceException exception) {
			log.warn("KTO detailCommon2 failed for contentId={}", place.externalPlaceId(), exception);
			return place;
		}
	}

	private List<String> loadPhotosSafely(TourismPlaceFeedItem place) {
		var cachedPhotos = photoCache.find(place);
		if (cachedPhotos.isPresent()) {
			return cachedPhotos.get();
		}
		try {
			List<String> photos = parseDetailImages(get(buildImageUri(place.externalPlaceId())));
			photoCache.put(place, photos);
			return photos;
		}
		catch (KtoTourismPlaceException exception) {
			log.warn("KTO detailImage2 failed for contentId={}", place.externalPlaceId(), exception);
			return List.of();
		}
	}

	private static TourismPlaceFeedItem withDescription(TourismPlaceFeedItem place, String description) {
		return new TourismPlaceFeedItem(
			place.externalPlaceId(),
			place.name(),
			place.address(),
			place.lat(),
			place.lng(),
			place.thumbnailUrl(),
			place.category(),
			description,
			place.photos(),
			place.sourceModifiedAt()
		);
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

	private URI buildImageUri(String contentId) {
		return commonUri("/detailImage2")
			.queryParam("contentId", contentId)
			.queryParam("imageYN", "Y")
			.queryParam("numOfRows", 3)
			.queryParam("pageNo", 1)
			.build(true)
			.toUri();
	}

	private URI buildIntroUri(String contentId, String contentTypeId) {
		UriComponentsBuilder builder = commonUri("/detailIntro2")
			.queryParam("contentId", contentId);
		if (contentTypeId != null && !contentTypeId.isBlank()) {
			builder.queryParam("contentTypeId", contentTypeId);
		}
		return builder.build(true).toUri();
	}

	private URI buildBarrierFreeUri(String contentId) {
		String barrierFreeBaseUrl = properties.getBaseUrl().replace("KorService2", "KorWithService2");
		return commonUri(barrierFreeBaseUrl, "/detailWithTour2")
			.queryParam("contentId", contentId)
			.build(true)
			.toUri();
	}

	private UriComponentsBuilder commonUri(String path) {
		return commonUri(properties.getBaseUrl(), path);
	}

	private UriComponentsBuilder commonUri(String baseUrl, String path) {
		return UriComponentsBuilder.fromUriString(baseUrl)
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
			List<String> photos = distinctUrls(preferredImage(item));
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
		String detailImage = preferredImage(detail);
		List<String> photos = detailImage == null ? place.photos() : List.of(detailImage);
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

	static List<String> parseDetailImages(JsonNode response) {
		JsonNode body = successfulBody(response);
		JsonNode items = body.path("items").path("item");
		if (!items.isArray()) {
			return List.of();
		}
		LinkedHashSet<String> photos = new LinkedHashSet<>();
		for (JsonNode item : items) {
			String license = text(item, "cpyrhtDivCd");
			if (!("Type1".equalsIgnoreCase(license) || "Type3".equalsIgnoreCase(license))) {
				continue;
			}
			String image = firstNonBlank(text(item, "originimgurl"), text(item, "smallimageurl"));
			if (image != null) {
				photos.add(image);
			}
			if (photos.size() == 3) {
				break;
			}
		}
		return List.copyOf(photos);
	}

	static TourismPlaceFeedItem withPhotos(
		TourismPlaceFeedItem place,
		String awardPhoto,
		List<String> detailPhotos
	) {
		LinkedHashSet<String> photos = new LinkedHashSet<>();
		addPhoto(photos, awardPhoto);
		addPhoto(photos, place.thumbnailUrl());
		place.photos().forEach(photo -> addPhoto(photos, photo));
		detailPhotos.forEach(photo -> addPhoto(photos, photo));
		List<String> selected = photos.stream().limit(3).toList();
		String thumbnail = selected.isEmpty() ? place.thumbnailUrl() : selected.getFirst();
		return new TourismPlaceFeedItem(
			place.externalPlaceId(), place.name(), place.address(), place.lat(), place.lng(),
			thumbnail, place.category(), place.description(), selected, place.sourceModifiedAt()
		);
	}

	private static void addPhoto(LinkedHashSet<String> photos, String photo) {
		if (photo != null && !photo.isBlank()) {
			photos.add(photo);
		}
	}

	static PlaceIntroRaw parseIntro(JsonNode response, String contentTypeId) {
		JsonNode body = successfulBody(response);
		JsonNode items = body.path("items").path("item");
		if (!items.isArray() || items.isEmpty()) {
			return PlaceIntroRaw.empty();
		}
		JsonNode item = items.get(0);
		String typeId = contentTypeId != null && !contentTypeId.isBlank()
			? contentTypeId
			: text(item, "contenttypeid");
		return switch (typeId == null ? "" : typeId) {
			case "12" -> new PlaceIntroRaw(
				text(item, "usetime"),
				text(item, "restdate"),
				text(item, "parking"),
				null,
				text(item, "chkbabycarriage"),
				text(item, "chkpet")
			);
			case "14" -> new PlaceIntroRaw(
				text(item, "usetimeculture"),
				text(item, "restdateculture"),
				text(item, "parkingculture"),
				null,
				text(item, "chkbabycarriageculture"),
				text(item, "chkpetculture")
			);
			case "28" -> new PlaceIntroRaw(
				text(item, "usetimeleports"),
				text(item, "restdateleports"),
				text(item, "parkingleports"),
				null,
				text(item, "chkbabycarriageleports"),
				text(item, "chkpetleports")
			);
			case "39" -> new PlaceIntroRaw(
				text(item, "opentimefood"),
				text(item, "restdatefood"),
				text(item, "parkingfood"),
				null,
				null,
				null
			);
			case "38" -> new PlaceIntroRaw(
				text(item, "opentime"),
				text(item, "restdateshopping"),
				text(item, "parkingshopping"),
				null,
				text(item, "chkbabycarriageshopping"),
				text(item, "chkpetshopping")
			);
			default -> new PlaceIntroRaw(
				text(item, "usetime"),
				text(item, "restdate"),
				text(item, "parking"),
				text(item, "handicap1"),
				text(item, "chkbabycarriage"),
				text(item, "chkpet")
			);
		};
	}

	static PlaceIntroRaw parseBarrierFree(JsonNode response) {
		JsonNode body = successfulBody(response);
		JsonNode items = body.path("items").path("item");
		if (!items.isArray() || items.isEmpty()) {
			return PlaceIntroRaw.empty();
		}
		JsonNode item = items.get(0);
		return new PlaceIntroRaw(
			null,
			null,
			null,
			joinNonBlank(
				text(item, "wheelchair"),
				text(item, "publictransport"),
				text(item, "exit"),
				text(item, "elevator"),
				text(item, "restroom"),
				text(item, "room"),
				text(item, "handicapetc")
			),
			text(item, "stroller"),
			null
		);
	}

	static PlaceIntroRaw mergeIntro(PlaceIntroRaw intro, PlaceIntroRaw barrierFree) {
		return new PlaceIntroRaw(
			intro.useTime(),
			intro.restDate(),
			firstNonBlank(intro.parking(), barrierFree.parking()),
			joinNonBlank(intro.disability(), barrierFree.disability()),
			firstNonBlank(intro.chkBabyCarriage(), barrierFree.chkBabyCarriage()),
			intro.chkPet()
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

	private static String preferredImage(JsonNode item) {
		String original = text(item, "firstimage");
		return original == null ? text(item, "firstimage2") : original;
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

	private static String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) return first;
		return second;
	}

	private static String joinNonBlank(String... values) {
		return java.util.stream.Stream.of(values)
			.filter(value -> value != null && !value.isBlank())
			.reduce((left, right) -> left + "\n" + right)
			.orElse(null);
	}

}
