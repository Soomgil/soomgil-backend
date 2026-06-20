package com.soomgil.geo.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.geo.application.command.dto.SyncLegalRegionsCommand;
import com.soomgil.geo.application.command.dto.SyncLegalRegionsResult;
import com.soomgil.geo.application.port.LegalRegionCommandRepository;
import com.soomgil.geo.application.port.LegalRegionSyncLog;
import com.soomgil.geo.application.port.LegalRegionUpsert;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SyncLegalRegionsCommand}를 처리해 법정동 CSV를 DB에 반영한다.
 */
@Component
public class SyncLegalRegionsHandler implements CommandHandler<SyncLegalRegionsCommand, SyncLegalRegionsResult> {

	private final LegalRegionCommandRepository repository;
	private final TimeProvider timeProvider;

	public SyncLegalRegionsHandler(LegalRegionCommandRepository repository, TimeProvider timeProvider) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Override
	@Transactional
	public SyncLegalRegionsResult handle(SyncLegalRegionsCommand command) {
		Instant startedAt = timeProvider.now();
		List<LegalRegionUpsert> regions = LegalRegionCsvParser.parse(
			new String(command.content(), command.charset()),
			startedAt
		);

		int insertedCount = 0;
		int updatedCount = 0;
		int deactivatedCount = 0;
		for (LegalRegionUpsert region : regions) {
			boolean exists = repository.existsLegalRegion(region.code());
			repository.upsertLegalRegion(region);
			if (exists) {
				updatedCount++;
			}
			else {
				insertedCount++;
			}
			if (!region.active()) {
				deactivatedCount++;
			}
		}

		Instant finishedAt = timeProvider.now();
		repository.saveSyncLog(new LegalRegionSyncLog(
			command.source(),
			command.sourceFileName(),
			regions.size(),
			insertedCount,
			updatedCount,
			deactivatedCount,
			startedAt,
			finishedAt,
			"SUCCESS",
			null
		));
		return new SyncLegalRegionsResult(regions.size(), insertedCount, updatedCount, deactivatedCount);
	}
}
