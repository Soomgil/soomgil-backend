package com.soomgil.global.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 개발과 배포 smoke check에서 사용하는 최소 health API.
 *
 * <p>비즈니스 readiness가 아니라 backend process가 HTTP 요청을 받을 수 있는지 확인하는 endpoint이다.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {

	/**
	 * backend process 상태를 반환한다.
	 *
	 * @return health status map
	 */
	@GetMapping
	public Map<String, String> health() {
		return Map.of(
			"status", "UP",
			"service", "soomgil-backend"
		);
	}
}
