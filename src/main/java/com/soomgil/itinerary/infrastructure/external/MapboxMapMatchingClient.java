package com.soomgil.itinerary.infrastructure.external;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.itinerary.application.port.MapMatchClientRequest;
import com.soomgil.itinerary.application.port.MapMatchClientResult;
import com.soomgil.itinerary.application.port.MapMatchingClient;
import com.soomgil.itinerary.application.port.MapMatchingException;
import com.soomgil.itinerary.application.port.RouteCoordinate;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Mapbox Map Matching API client.
 */
@Component
public class MapboxMapMatchingClient implements MapMatchingClient {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final MapboxProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public MapboxMapMatchingClient(MapboxProperties properties, ObjectMapper objectMapper) {
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.restClient = RestClient.builder().build();
	}

	@Override
	public MapMatchClientResult match(MapMatchClientRequest request) {
		if (properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
			throw new MapMatchingException("CONFIGURATION_ERROR", "Mapbox access token is not configured.");
		}
		URI uri = buildUri(request);
		JsonNode response;
		try {
			response = restClient.get()
				.uri(uri)
				.retrieve()
				.body(JsonNode.class);
		}
		catch (RestClientResponseException exception) {
			throw toMapMatchingException(exception);
		}
		catch (RestClientException exception) {
			throw new MapMatchingException("REQUEST_FAILED", exception.getMessage());
		}
		if (response == null) {
			throw new MapMatchingException("EMPTY_RESPONSE", "Mapbox response is empty.");
		}
		String code = response.path("code").asText("UNKNOWN");
		if (!"Ok".equals(code)) {
			throw new MapMatchingException(code, response.path("message").asText("Mapbox route matching failed."));
		}
		JsonNode matchings = response.path("matchings");
		if (!matchings.isArray() || matchings.isEmpty()) {
			throw new MapMatchingException("NO_MATCHING", "Mapbox response does not contain matching result.");
		}
		JsonNode firstMatching = matchings.get(0);
		JsonNode geometry = firstMatching.path("geometry");
		if (geometry.isMissingNode() || geometry.isNull() || geometry.isEmpty()) {
			throw new MapMatchingException("NO_GEOMETRY", "Mapbox response does not contain route geometry.");
		}

		return new MapMatchClientResult(
			objectMapper.convertValue(geometry, MAP_TYPE),
			tracepoints(response.path("tracepoints")),
			metadata(response, firstMatching),
			nullableDouble(firstMatching, "distance"),
			nullableDouble(firstMatching, "duration"),
			nullableDouble(firstMatching, "confidence")
		);
	}

	private URI buildUri(MapMatchClientRequest request) {
		String coordinates = request.coordinates().stream()
			.map(this::formatCoordinate)
			.collect(Collectors.joining(";"));
		UriComponentsBuilder builder = UriComponentsBuilder
			.fromUriString(properties.getBaseUrl())
			.path("/matching/v5/")
			.path(request.providerProfile())
			.path("/")
			.path(coordinates + ".json")
			.queryParam("access_token", properties.getAccessToken())
			.queryParam("geometries", "geojson")
			.queryParam("overview", "full");
		if (request.tidy() != null) {
			builder.queryParam("tidy", request.tidy());
		}
		if (request.radiuses() != null) {
			builder.queryParam("radiuses", request.radiuses().stream()
				.map(radius -> radius == null ? "" : radius.toString())
				.collect(Collectors.joining(";")));
		}
		return builder.build(true).toUri();
	}

	private String formatCoordinate(RouteCoordinate coordinate) {
		return coordinate.lng() + "," + coordinate.lat();
	}

	private MapMatchingException toMapMatchingException(RestClientResponseException exception) {
		try {
			JsonNode body = objectMapper.readTree(exception.getResponseBodyAsString());
			return new MapMatchingException(
				body.path("code").asText("HTTP_" + exception.getStatusCode().value()),
				body.path("message").asText(exception.getMessage())
			);
		}
		catch (Exception parseException) {
			return new MapMatchingException("HTTP_" + exception.getStatusCode().value(), exception.getMessage());
		}
	}

	private List<Map<String, Object>> tracepoints(JsonNode tracepoints) {
		if (!tracepoints.isArray()) {
			return List.of();
		}
		List<Map<String, Object>> result = new ArrayList<>();
		for (JsonNode tracepoint : tracepoints) {
			result.add(tracepoint == null || tracepoint.isNull() ? null : objectMapper.convertValue(tracepoint, MAP_TYPE));
		}
		return result;
	}

	private Map<String, Object> metadata(JsonNode response, JsonNode firstMatching) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("code", response.path("code").asText());
		result.put("matchingCount", response.path("matchings").size());
		result.put("confidence", nullableDouble(firstMatching, "confidence"));
		result.put("distance", nullableDouble(firstMatching, "distance"));
		result.put("duration", nullableDouble(firstMatching, "duration"));
		result.put("weight", nullableDouble(firstMatching, "weight"));
		result.put("weightName", firstMatching.path("weight_name").isMissingNode()
			? null
			: firstMatching.path("weight_name").asText());
		return result;
	}

	private Double nullableDouble(JsonNode node, String fieldName) {
		JsonNode field = node.path(fieldName);
		return field.isNumber() ? field.asDouble() : null;
	}
}
