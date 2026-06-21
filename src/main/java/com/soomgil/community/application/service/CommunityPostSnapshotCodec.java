package com.soomgil.community.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** 게시글의 immutable 일정 snapshot을 JSONB 문자열로 직렬화하고 복원한다. */
@Component
public class CommunityPostSnapshotCodec {

	private final ObjectMapper objectMapper;

	public CommunityPostSnapshotCodec(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	public String encode(CommunityPostSnapshot snapshot) {
		try {
			return objectMapper.writeValueAsString(Objects.requireNonNull(snapshot, "snapshot must not be null"));
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Community post snapshot could not be serialized.", exception);
		}
	}

	public CommunityPostSnapshot decode(String json) {
		try {
			return objectMapper.readValue(json, CommunityPostSnapshot.class);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Community post snapshot could not be deserialized.", exception);
		}
	}
}
