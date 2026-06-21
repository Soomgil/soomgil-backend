package com.soomgil.media.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.media.api.dto.CreateMediaFileRequest;
import com.soomgil.media.api.dto.CreateUploadUrlRequest;
import com.soomgil.media.application.command.dto.CreateMediaFileCommand;
import com.soomgil.media.application.command.dto.CreateUploadUrlCommand;
import com.soomgil.media.application.command.dto.DeleteMediaFileCommand;
import com.soomgil.media.application.command.dto.UploadUrlView;
import com.soomgil.media.application.command.handler.CreateMediaFileCommandHandler;
import com.soomgil.media.application.command.handler.CreateUploadUrlCommandHandler;
import com.soomgil.media.application.command.handler.DeleteMediaFileCommandHandler;
import com.soomgil.media.domain.model.MediaFileMetadata;
import com.soomgil.media.domain.model.MediaPurpose;
import java.net.URI;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MediaControllerTest {

	private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID MEDIA_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private CreateUploadUrlCommandHandler uploadHandler;
	private CreateMediaFileCommandHandler createHandler;
	private DeleteMediaFileCommandHandler deleteHandler;
	private ObjectMapper objectMapper;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		uploadHandler = mock(CreateUploadUrlCommandHandler.class);
		createHandler = mock(CreateMediaFileCommandHandler.class);
		deleteHandler = mock(DeleteMediaFileCommandHandler.class);
		objectMapper = Jackson2ObjectMapperBuilder.json()
			.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.build();
		mockMvc = MockMvcBuilders.standaloneSetup(new MediaController(uploadHandler, createHandler, deleteHandler))
			.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
			.build();
	}

	@Test
	void createsUploadUrlUsingContractPath() throws Exception {
		AtomicReference<CreateUploadUrlCommand> captured = new AtomicReference<>();
		when(uploadHandler.handle(any())).thenAnswer(invocation -> {
			captured.set(invocation.getArgument(0));
			return new UploadUrlView(
				URI.create("https://storage.example.com/upload"), "PUT", "media/key.jpg",
				Map.of("Content-Type", "image/jpeg"), OffsetDateTime.parse("2026-06-20T12:10:00Z")
			);
		});

		mockMvc.perform(post("/api/v1/media/upload-urls")
				.principal(principal())
				.contentType("application/json")
				.content(objectMapper.writeValueAsString(new CreateUploadUrlRequest(
					"avatar.jpg", "image/jpeg", 1024L, MediaPurpose.PROFILE_IMAGE
				))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.method").value("PUT"))
			.andExpect(jsonPath("$.objectKey").value("media/key.jpg"));

		assertThat(captured.get().userId()).isEqualTo(USER_ID);
		assertThat(captured.get().purpose().name()).isEqualTo("PROFILE_IMAGE");
	}

	@Test
	void registersMediaFileUsingContractPath() throws Exception {
		when(createHandler.handle(any())).thenReturn(mediaFile());

		mockMvc.perform(post("/api/v1/media/files")
				.principal(principal())
				.contentType("application/json")
				.content(objectMapper.writeValueAsString(new CreateMediaFileRequest(
					"media/key.jpg", null, "image/jpeg", 1024L, 100, 100, null, null
				))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(MEDIA_ID.toString()))
			.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void deletesMediaFileUsingContractPath() throws Exception {
		AtomicReference<DeleteMediaFileCommand> captured = new AtomicReference<>();
		when(deleteHandler.handle(any())).thenAnswer(invocation -> {
			captured.set(invocation.getArgument(0));
			return NoResult.INSTANCE;
		});

		mockMvc.perform(delete("/api/v1/media/files/{mediaFileId}", MEDIA_ID).principal(principal()))
			.andExpect(status().isNoContent());

		assertThat(captured.get().userId()).isEqualTo(USER_ID);
		assertThat(captured.get().mediaFileId()).isEqualTo(MEDIA_ID);
	}

	private Principal principal() {
		return () -> USER_ID.toString();
	}

	private MediaFileMetadata mediaFile() {
		return new MediaFileMetadata(
			MEDIA_ID, USER_ID, "S3_COMPATIBLE", "bucket", new StorageObjectKey("media/key.jpg"),
			URI.create("https://cdn.example.com/media/key.jpg"), "image/jpeg", 1024L,
			100, 100, null, null, "ACTIVE", OffsetDateTime.parse("2026-06-20T12:00:00Z"), null, null
		);
	}
}
