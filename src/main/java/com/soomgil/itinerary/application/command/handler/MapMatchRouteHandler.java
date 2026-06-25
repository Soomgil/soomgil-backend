package com.soomgil.itinerary.application.command.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.MapMatchRouteCommand;
import com.soomgil.itinerary.application.command.dto.MapMatchRouteResult;
import com.soomgil.itinerary.application.command.dto.SaveRouteSegmentCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.MapMatchClientRequest;
import com.soomgil.itinerary.application.port.MapMatchClientResult;
import com.soomgil.itinerary.application.port.MapMatchingClient;
import com.soomgil.itinerary.application.port.MapMatchingException;
import com.soomgil.itinerary.application.port.RouteCoordinate;
import com.soomgil.itinerary.application.port.RouteMatchRequestLog;
import com.soomgil.itinerary.domain.model.RouteMode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mapbox map matching 결과로 route segment를 생성한다.
 */
@Component
public class MapMatchRouteHandler implements CommandHandler<MapMatchRouteCommand, MapMatchRouteResult> {

	private static final String PROVIDER = "MAPBOX";
	private static final String FALLBACK_PROVIDER = "USER_TRACE";

	private final ItineraryCommandRepository repository;
	private final TripAccessGuard tripAccessGuard;
	private final MapMatchingClient mapMatchingClient;
	private final SaveRouteSegmentHandler saveRouteSegmentHandler;
	private final TimeProvider timeProvider;
	private final ObjectMapper objectMapper;

	public MapMatchRouteHandler(
		ItineraryCommandRepository repository,
		TripAccessGuard tripAccessGuard,
		MapMatchingClient mapMatchingClient,
		SaveRouteSegmentHandler saveRouteSegmentHandler,
		TimeProvider timeProvider,
		ObjectMapper objectMapper
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.mapMatchingClient = Objects.requireNonNull(mapMatchingClient, "mapMatchingClient must not be null");
		this.saveRouteSegmentHandler = Objects.requireNonNull(
			saveRouteSegmentHandler,
			"saveRouteSegmentHandler must not be null"
		);
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	@Transactional
	public MapMatchRouteResult handle(MapMatchRouteCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validate(command);
		String providerProfile = providerProfile(command.mode());
		MapMatchClientRequest clientRequest = new MapMatchClientRequest(
			providerProfile,
			command.coordinates(),
			command.radiuses(),
			command.tidy()
		);

		MapMatchClientResult clientResult;
		try {
			clientResult = mapMatchingClient.match(clientRequest);
		}
		catch (MapMatchingException exception) {
			ItineraryMutationResult fallbackMutation = saveRouteSegmentHandler.handle(new SaveRouteSegmentCommand(
				command.tripId(),
				command.actorUserId(),
				command.baseVersion(),
				command.originItineraryItemId(),
				command.destinationItineraryItemId(),
				command.mode(),
				FALLBACK_PROVIDER,
				fallbackProviderProfile(command.mode()),
				rawLineStringGeometry(command.coordinates()),
				null,
				null,
				null
			));
			Long requestId = saveMatchLog(
				command,
				providerProfile,
				fallbackMutation.route().id(),
				"FAILED",
				null,
				exception
			);
			return new MapMatchRouteResult(
				fallbackMutation,
				requestId,
				List.of(),
				fallbackMetadata(exception)
			);
		}

		ItineraryMutationResult mutation = saveRouteSegmentHandler.handle(new SaveRouteSegmentCommand(
			command.tripId(),
			command.actorUserId(),
			command.baseVersion(),
			command.originItineraryItemId(),
			command.destinationItineraryItemId(),
			command.mode(),
			PROVIDER,
			providerProfile,
			clientResult.geometry(),
			clientResult.distanceMeters(),
			clientResult.durationSeconds(),
			clientResult.confidence()
		));
		Long requestId = saveMatchLog(
			command,
			providerProfile,
			mutation.route().id(),
			"SUCCEEDED",
			clientResult,
			null
		);
		return new MapMatchRouteResult(
			mutation,
			requestId,
			clientResult.tracepoints(),
			clientResult.matchingsMetadata()
		);
	}

	private void validate(MapMatchRouteCommand command) {
		if (command.originItineraryItemId() == null || command.destinationItineraryItemId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route origin and destination are required.");
		}
		if (command.originItineraryItemId().equals(command.destinationItineraryItemId())) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route origin and destination must be different.");
		}
		if (command.mode() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route mode is required.");
		}
		if (command.coordinates() == null || command.coordinates().size() < 2 || command.coordinates().size() > 25) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route coordinates must contain 2 to 25 points.");
		}
		for (RouteCoordinate coordinate : command.coordinates()) {
			if (coordinate == null) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route coordinates must not contain null.");
			}
		}
		if (command.radiuses() != null) {
			if (command.radiuses().size() != command.coordinates().size()) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route radiuses must match coordinates size.");
			}
			for (Double radius : command.radiuses()) {
				if (radius != null && (radius < 0 || radius > 50)) {
					throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route radius must be between 0 and 50.");
				}
			}
		}
	}

	private Long saveMatchLog(
		MapMatchRouteCommand command,
		String providerProfile,
		java.util.UUID routeId,
		String status,
		MapMatchClientResult result,
		MapMatchingException exception
	) {
		Instant now = timeProvider.now();
		String inputCoordinates = toJson(command.coordinates());
		String radiuses = command.radiuses() == null ? null : toJson(command.radiuses());
		String tracepoints = result == null ? null : toJson(result.tracepoints());
		String matchingsMetadata = result == null ? null : toJson(result.matchingsMetadata());
		return repository.insertRouteMatchRequest(new RouteMatchRequestLog(
			command.tripId(),
			routeId,
			command.originItineraryItemId(),
			command.destinationItineraryItemId(),
			command.actorUserId(),
			PROVIDER,
			providerProfile,
			inputCoordinates,
			radiuses,
			command.tidy(),
			hash(providerProfile + "|" + inputCoordinates + "|" + radiuses + "|" + command.tidy()),
			status,
			result == null ? null : result.confidence(),
			result == null ? null : result.distanceMeters(),
			result == null ? null : result.durationSeconds(),
			tracepoints,
			matchingsMetadata,
			exception == null ? null : exception.providerCode(),
			exception == null ? null : exception.getMessage(),
			now,
			now
		));
	}

	private String providerProfile(RouteMode mode) {
		return switch (mode) {
			case DRIVING -> "mapbox/driving";
			case WALKING -> "mapbox/walking";
		};
	}

	private String fallbackProviderProfile(RouteMode mode) {
		return switch (mode) {
			case DRIVING -> "user-trace/driving";
			case WALKING -> "user-trace/walking";
		};
	}

	private Map<String, Object> fallbackMetadata(MapMatchingException exception) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("code", "FALLBACK");
		metadata.put("reason", exception.providerCode());
		metadata.put("message", exception.getMessage());
		return metadata;
	}

	private Map<String, Object> rawLineStringGeometry(List<RouteCoordinate> coordinates) {
		List<List<Double>> lineCoordinates = new ArrayList<>();
		for (RouteCoordinate coordinate : coordinates) {
			lineCoordinates.add(List.of(coordinate.lng(), coordinate.lat()));
		}
		return Map.of(
			"type", "LineString",
			"coordinates", lineCoordinates
		);
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException exception) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route matching payload is invalid.");
		}
	}

	private String hash(String source) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available.", exception);
		}
	}
}
