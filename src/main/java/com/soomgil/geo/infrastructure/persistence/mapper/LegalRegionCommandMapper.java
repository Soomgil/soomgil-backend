package com.soomgil.geo.infrastructure.persistence.mapper;

import com.soomgil.geo.application.port.LegalRegionSyncLog;
import com.soomgil.geo.application.port.LegalRegionUpsert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 법정동 지역 쓰기 SQL mapper.
 */
@Mapper
public interface LegalRegionCommandMapper {

	/**
	 * 법정동 지역 존재 여부를 조회한다.
	 *
	 * @param code 법정동 code
	 * @return 존재 여부
	 */
	boolean existsLegalRegion(@Param("code") String code);

	/**
	 * 법정동 지역을 code 기준으로 추가하거나 갱신한다.
	 *
	 * @param region upsert할 지역
	 */
	void upsertLegalRegion(@Param("region") LegalRegionUpsert region);

	/**
	 * 법정동 지역 동기화 로그를 저장한다.
	 *
	 * @param log 동기화 로그
	 */
	void insertSyncLog(@Param("log") LegalRegionSyncLog log);
}
