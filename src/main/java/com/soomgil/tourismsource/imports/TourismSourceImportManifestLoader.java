package com.soomgil.tourismsource.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 관광 원천 import manifest를 classpath 또는 resource에서 읽는다.
 */
public class TourismSourceImportManifestLoader {

	private final ObjectMapper objectMapper;

	public TourismSourceImportManifestLoader() {
		this(JsonMapper.builder()
			.addModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.build());
	}

	public TourismSourceImportManifestLoader(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * classpath 위치에서 manifest를 읽고 검증한다.
	 *
	 * @param classpathLocation classpath 위치
	 * @return 검증된 manifest
	 */
	public TourismSourceImportManifest load(String classpathLocation) {
		return load(new ClassPathResource(classpathLocation));
	}

	/**
	 * resource에서 manifest를 읽고 검증한다.
	 *
	 * @param resource manifest resource
	 * @return 검증된 manifest
	 */
	public TourismSourceImportManifest load(Resource resource) {
		try (InputStream inputStream = resource.getInputStream()) {
			TourismSourceImportManifest manifest = objectMapper.readValue(
				inputStream,
				TourismSourceImportManifest.class
			);
			manifest.validate();
			return manifest;
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to load tourism source import manifest.", exception);
		}
	}
}
