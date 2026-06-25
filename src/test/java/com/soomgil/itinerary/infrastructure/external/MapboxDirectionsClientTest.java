package com.soomgil.itinerary.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.itinerary.application.port.MapMatchClientRequest;
import com.soomgil.itinerary.application.port.RouteCoordinate;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MapboxDirectionsClientTest {

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void sendsDirectionsRequestAndParsesFirstRoute() throws Exception {
		AtomicReference<URI> requestedUri = new AtomicReference<>();
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/directions/v5/", exchange -> {
			requestedUri.set(exchange.getRequestURI());
			byte[] body = ("""
				{
				  "code": "Ok",
				  "waypoints": [{"distance": 0}, {"distance": 1}],
				  "routes": [{
				    "geometry": {"type": "LineString", "coordinates": [[127.0, 37.0], [127.1, 37.1]]},
				    "distance": 120.5,
				    "duration": 60.25
				  }]
				}
				""").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();

		MapboxProperties properties = new MapboxProperties();
		properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
		properties.setAccessToken("test-token");
		MapboxDirectionsClient client = new MapboxDirectionsClient(properties, new ObjectMapper());

		var result = client.match(new MapMatchClientRequest(
			"mapbox/walking",
			List.of(new RouteCoordinate(127.0, 37.0), new RouteCoordinate(127.1, 37.1)),
			List.of(10.0, 20.0),
			true
		));

		assertThat(requestedUri.get().getPath())
			.isEqualTo("/directions/v5/mapbox/walking/127.0,37.0;127.1,37.1.json");
		assertThat(requestedUri.get().getQuery())
			.contains("access_token=test-token", "geometries=geojson", "overview=full", "steps=false")
			.doesNotContain("radiuses=", "tidy=");
		assertThat(result.geometry()).containsEntry("type", "LineString");
		assertThat(result.tracepoints()).hasSize(2);
		assertThat(result.distanceMeters()).isEqualTo(120.5);
		assertThat(result.durationSeconds()).isEqualTo(60.25);
		assertThat(result.confidence()).isNull();
	}
}
