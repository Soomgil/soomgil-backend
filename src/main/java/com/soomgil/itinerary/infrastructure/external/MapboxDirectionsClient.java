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
 * Mapbox Directions API client.
 */
@Component
public class MapboxDirectionsClient implements MapMatchingClient {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final MapboxProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public MapboxDirectionsClient(MapboxProperties properties, ObjectMapper objectMapper) {
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
			throw new MapMatchingException(code, response.path("message").asText("Mapbox directions request failed."));
		}
		JsonNode routes = response.path("routes");
		if (!routes.isArray() || routes.isEmpty()) {
			throw new MapMatchingException("NO_ROUTE", "Mapbox response does not contain route result.");
		}
		JsonNode firstRoute = routes.get(0);
		JsonNode geometry = firstRoute.path("geometry");
		if (geometry.isMissingNode() || geometry.isNull() || geometry.isEmpty()) {
			throw new MapMatchingException("NO_GEOMETRY", "Mapbox response does not contain route geometry.");
		}

		return new MapMatchClientResult(
			objectMapper.convertValue(geometry, MAP_TYPE),
			waypoints(response.path("waypoints")),
			metadata(response, firstRoute),
			nullableDouble(firstRoute, "distance"),
			nullableDouble(firstRoute, "duration"),
			null
		);
	}

	private URI buildUri(MapMatchClientRequest request) {
		String coordinates = request.coordinates().stream()
			.map(this::formatCoordinate)
			.collect(Collectors.joining(";"));
		UriComponentsBuilder builder = UriComponentsBuilder
			.fromUriString(properties.getBaseUrl())
			.path("/directions/v5/")
			.path(request.providerProfile())
			.path("/")
			.path(coordinates + ".json")
			.queryParam("access_token", properties.getAccessToken())
			.queryParam("geometries", "geojson")
			.queryParam("overview", "full")
			.queryParam("steps", "false");
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

	private List<Map<String, Object>> waypoints(JsonNode waypoints) {
		if (!waypoints.isArray()) {
			return List.of();
		}
		List<Map<String, Object>> result = new ArrayList<>();
		for (JsonNode waypoint : waypoints) {
			result.add(waypoint == null || waypoint.isNull() ? null : objectMapper.convertValue(waypoint, MAP_TYPE));
		}
		return result;
	}

	private Map<String, Object> metadata(JsonNode response, JsonNode firstRoute) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("code", response.path("code").asText());
		result.put("routeCount", response.path("routes").size());
		result.put("distance", nullableDouble(firstRoute, "distance"));
		result.put("duration", nullableDouble(firstRoute, "duration"));
		result.put("weight", nullableDouble(firstRoute, "weight"));
		result.put("weightName", firstRoute.path("weight_name").isMissingNode()
			? null
			: firstRoute.path("weight_name").asText());
		return result;
	}

	private Double nullableDouble(JsonNode node, String fieldName) {
		JsonNode field = node.path(fieldName);
		return field.isNumber() ? field.asDouble() : null;
	}
}
