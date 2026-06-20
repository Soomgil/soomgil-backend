package com.soomgil.geo.application.port;

/**
 * 법정동 지역 쓰기 persistence 계약.
 */
public interface LegalRegionCommandRepository {

	/**
	 * 법정동 지역 code 존재 여부를 조회한다.
	 *
	 * @param code 법정동코드 10자리
	 * @return 존재하면 true
	 */
	boolean existsLegalRegion(String code);

	/**
	 * 법정동 지역 row를 upsert한다.
	 *
	 * @param region 저장할 법정동 지역
	 */
	void upsertLegalRegion(LegalRegionUpsert region);

	/**
	 * 법정동 동기화 이력을 저장한다.
	 *
	 * @param log 저장할 동기화 이력
	 */
	void saveSyncLog(LegalRegionSyncLog log);
}
