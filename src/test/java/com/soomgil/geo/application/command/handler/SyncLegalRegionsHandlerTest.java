package com.soomgil.geo.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.geo.application.command.dto.SyncLegalRegionsCommand;
import com.soomgil.geo.application.command.dto.SyncLegalRegionsResult;
import com.soomgil.geo.application.port.LegalRegionCommandRepository;
import com.soomgil.geo.application.port.LegalRegionSyncLog;
import com.soomgil.geo.application.port.LegalRegionUpsert;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SyncLegalRegionsHandlerTest {

	@Test
	void syncsLegalRegionsAndStoresLog() {
		CapturingLegalRegionCommandRepository repository = new CapturingLegalRegionCommandRepository();
		repository.existingCodes.add("1111000000");
		SyncLegalRegionsHandler handler = new SyncLegalRegionsHandler(
			repository,
			() -> Instant.parse("2026-06-17T00:00:00Z")
		);
		String content = """
			법정동코드	법정동명	폐지여부
			1100000000	서울특별시	존재
			1111000000	서울특별시 종로구	존재
			1111010100	서울특별시 종로구 청운동	폐지
			""";

		SyncLegalRegionsResult result = handler.handle(new SyncLegalRegionsCommand(
			"government",
			"legal-regions.tsv",
			content.getBytes(StandardCharsets.UTF_8),
			StandardCharsets.UTF_8
		));

		assertThat(result.totalCount()).isEqualTo(3);
		assertThat(result.insertedCount()).isEqualTo(2);
		assertThat(result.updatedCount()).isEqualTo(1);
		assertThat(result.deactivatedCount()).isEqualTo(1);
		assertThat(repository.upserts).hasSize(3);
		assertThat(repository.lastLog).satisfies(log -> {
			assertThat(log.source()).isEqualTo("government");
			assertThat(log.sourceFileName()).isEqualTo("legal-regions.tsv");
			assertThat(log.totalCount()).isEqualTo(3);
			assertThat(log.insertedCount()).isEqualTo(2);
			assertThat(log.updatedCount()).isEqualTo(1);
			assertThat(log.deactivatedCount()).isEqualTo(1);
			assertThat(log.status()).isEqualTo("SUCCESS");
		});
	}

	private static class CapturingLegalRegionCommandRepository implements LegalRegionCommandRepository {

		private final Set<String> existingCodes = new HashSet<>();
		private final List<LegalRegionUpsert> upserts = new ArrayList<>();
		private LegalRegionSyncLog lastLog;

		@Override
		public boolean existsLegalRegion(String code) {
			return existingCodes.contains(code);
		}

		@Override
		public void upsertLegalRegion(LegalRegionUpsert region) {
			upserts.add(region);
		}

		@Override
		public void saveSyncLog(LegalRegionSyncLog log) {
			this.lastLog = log;
		}
	}
}
