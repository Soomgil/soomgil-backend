package com.soomgil.geo.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.geo.api.dto.SyncLegalRegionsResponse;
import com.soomgil.geo.application.command.handler.SyncLegalRegionsHandler;
import com.soomgil.geo.application.port.LegalRegionCommandRepository;
import com.soomgil.geo.application.port.LegalRegionSyncLog;
import com.soomgil.geo.application.port.LegalRegionUpsert;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class GeoAdminControllerTest {

	@Test
	void syncLegalRegionsReturnsCounts() {
		CapturingLegalRegionCommandRepository repository = new CapturingLegalRegionCommandRepository();
		GeoAdminController controller = new GeoAdminController(new SyncLegalRegionsHandler(
			repository,
			() -> java.time.Instant.parse("2026-06-17T00:00:00Z")
		));
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"legal-regions.tsv",
			"text/tab-separated-values",
			("""
				법정동코드	법정동명	폐지여부
				1100000000	서울특별시	존재
				1111000000	서울특별시 종로구	존재
				1111010100	서울특별시 종로구 청운동	존재
				""").getBytes(StandardCharsets.UTF_8)
		);

		SyncLegalRegionsResponse result = controller.syncLegalRegions(file, "government");

		assertThat(result.totalCount()).isEqualTo(3);
		assertThat(result.insertedCount()).isEqualTo(3);
		assertThat(repository.lastLog.source()).isEqualTo("government");
		assertThat(repository.lastLog.sourceFileName()).isEqualTo("legal-regions.tsv");
	}

	private static class CapturingLegalRegionCommandRepository implements LegalRegionCommandRepository {

		private LegalRegionSyncLog lastLog;

		@Override
		public boolean existsLegalRegion(String code) {
			return false;
		}

		@Override
		public void upsertLegalRegion(LegalRegionUpsert region) {
		}

		@Override
		public void saveSyncLog(LegalRegionSyncLog log) {
			this.lastLog = log;
		}
	}
}
