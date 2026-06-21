package com.soomgil.community.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommunityPostSnapshotCodecTest {

	@Test
	void roundTripsAnImmutableSnapshot() {
		CommunityPostSnapshotCodec codec = new CommunityPostSnapshotCodec(new ObjectMapper().findAndRegisterModules());
		CommunityPostSnapshot snapshot = new CommunityPostSnapshot(List.of(), List.of(), null);

		assertThat(codec.decode(codec.encode(snapshot))).isEqualTo(snapshot);
	}
}
