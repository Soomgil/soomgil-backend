package com.soomgil.community.infrastructure.persistence.mapper;

import com.soomgil.community.domain.model.ReportReasonRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * {@code community.report_reasons} 테이블 mapper.
 *
 * <p>활성 신고 사유 목록을 조회한다.
 */
@Mapper
public interface ReportReasonMapper {

	/**
	 * 활성({@code is_active=true}) 신고 사유를 {@code sort_order} 오름차순으로 조회한다.
	 *
	 * @return 활성 신고 사유 목록
	 */
	@Select("""
		SELECT code, display_name, is_active, sort_order
		FROM community.report_reasons
		WHERE is_active = true
		ORDER BY sort_order
		""")
	List<ReportReasonRecord> findAllActive();
}
