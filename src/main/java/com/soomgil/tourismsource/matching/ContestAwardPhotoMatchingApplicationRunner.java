package com.soomgil.tourismsource.matching;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 설정이 켜졌을 때 애플리케이션 시작 시 pending 수상작 사진 매칭을 실행한다.
 */
@Component
@ConditionalOnProperty(
	prefix = "soomgil.tourism-source.award-photo-matching",
	name = "enabled",
	havingValue = "true"
)
public class ContestAwardPhotoMatchingApplicationRunner implements ApplicationRunner {

	private final ContestAwardPhotoMatchingBatchService batchService;
	private final int limit;

	public ContestAwardPhotoMatchingApplicationRunner(
		ContestAwardPhotoMatchingBatchService batchService,
		@Value("${soomgil.tourism-source.award-photo-matching.limit:100}") int limit
	) {
		this.batchService = batchService;
		this.limit = limit;
	}

	@Override
	public void run(ApplicationArguments args) {
		batchService.runPending(limit);
	}
}
