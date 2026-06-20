package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEnrichmentCandidateInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEnrichmentInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEnrichmentTagInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.PreferenceTagLookupRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * preference 장소 태깅 실행 결과 SQL mapper.
 */
@Mapper
public interface PreferencePlaceTagEnrichmentMapper {

	List<PreferenceTagLookupRow> findTagsByCodes(@Param("codes") List<String> codes);

	void insertEnrichment(PlaceTagEnrichmentInsertRow row);

	void insertCandidate(PlaceTagEnrichmentCandidateInsertRow row);

	void insertSelectedTag(PlaceTagEnrichmentTagInsertRow row);
}
