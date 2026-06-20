package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.SyntheticPersonaInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.SyntheticPersonaTagPreferenceInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.SyntheticPlaceTagSourceRow;
import com.soomgil.preference.infrastructure.persistence.row.SyntheticSwipeEventInsertRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 합성 페르소나 catalog와 실제 사용자 이벤트와 분리된 합성 이벤트를 저장하는 MyBatis mapper.
 */
@Mapper
public interface PreferenceSyntheticPersonaMapper {

	void upsertPersonas(@Param("rows") List<SyntheticPersonaInsertRow> rows);

	void deleteTagPreferences(@Param("generatorVersion") String generatorVersion);

	int insertTagPreferences(@Param("rows") List<SyntheticPersonaTagPreferenceInsertRow> rows);

	List<SyntheticPlaceTagSourceRow> findPlaceTagSources(@Param("limit") int limit);

	void upsertEvents(@Param("rows") List<SyntheticSwipeEventInsertRow> rows);
}
