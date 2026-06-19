package com.soomgil.preference;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PreferenceTagSeedIntegrationTest {

	private static final String DICTIONARY_VERSION = "preference-tags-v1";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void seedsFixedPreferenceTagWhitelistExactly() {
		List<TagSeedRow> actualRows = jdbcTemplate.query("""
			SELECT
				code,
				display_name,
				group_code,
				tag_type,
				is_selectable,
				is_active,
				dictionary_version
			FROM preference.preference_tags
			""",
			(resultSet, rowNumber) -> new TagSeedRow(
				resultSet.getString("code"),
				resultSet.getString("display_name"),
				resultSet.getString("group_code"),
				resultSet.getString("tag_type"),
				resultSet.getBoolean("is_selectable"),
				resultSet.getBoolean("is_active"),
				resultSet.getString("dictionary_version")
			));

		assertThat(actualRows).containsExactlyInAnyOrderElementsOf(expectedRows());
	}

	private static List<TagSeedRow> expectedRows() {
		return Arrays.stream("""
			nature_scene|자연/경관|nature_scene|GROUP|false
			nature|자연|nature_scene|TAG|true
			park|공원|nature_scene|TAG|true
			arboretum|수목원|nature_scene|TAG|true
			garden|정원|nature_scene|TAG|true
			forest|숲|nature_scene|TAG|true
			mountain|산|nature_scene|TAG|true
			coast|바다/해안|nature_scene|TAG|true
			island|섬|nature_scene|TAG|true
			lake_pond|연못/호수|nature_scene|TAG|true
			waterfront|수변경관|nature_scene|TAG|true
			valley_stream|계곡|nature_scene|TAG|true
			waterfall|폭포|nature_scene|TAG|true
			flower_plant|꽃/식물|nature_scene|TAG|true
			scenic_view|풍경좋은|nature_scene|TAG|true
			rural_landscape|농촌풍경|nature_scene|TAG|true
			fishing_village|어촌/포구|nature_scene|TAG|true
			night_view|야경|nature_scene|TAG|true
			sunset|노을|nature_scene|TAG|true
			stargazing|별보기|nature_scene|TAG|true
			autumn_foliage|단풍|nature_scene|TAG|true
			snow_scene|설경|nature_scene|TAG|true
			history_culture|역사/문화|history_culture|GROUP|false
			history|역사|history_culture|TAG|true
			traditional|전통적인|history_culture|TAG|true
			traditional_architecture|전통건축|history_culture|TAG|true
			palace_fortress|궁궐/성곽|history_culture|TAG|true
			temple_shrine|사찰/종교공간|history_culture|TAG|true
			heritage_site|문화유산|history_culture|TAG|true
			local_culture|지역문화|history_culture|TAG|true
			museum|박물관|history_culture|TAG|true
			gallery_exhibition|전시/미술|history_culture|TAG|true
			science_education|과학/교육|history_culture|TAG|true
			cultural_space|문화공간|history_culture|TAG|true
			performance_venue|공연공간|history_culture|TAG|true
			architecture|건축|history_culture|TAG|true
			industrial_heritage|산업유산/재생공간|history_culture|TAG|true
			activity|활동|activity|GROUP|false
			walking|산책|activity|TAG|true
			hiking|등산|activity|TAG|true
			cycling|자전거|activity|TAG|true
			photo_spot|사진명소|activity|TAG|true
			viewing|관람|activity|TAG|true
			hands_on_experience|체험|activity|TAG|true
			learning|학습|activity|TAG|true
			performance_viewing|공연관람|activity|TAG|true
			picnic|피크닉|activity|TAG|true
			leisure_activity|레저활동|activity|TAG|true
			water_activity|수상활동|activity|TAG|true
			camping|캠핑|activity|TAG|true
			hot_spring|온천|activity|TAG|true
			animal_viewing|동물관람|activity|TAG|true
			rides|놀이기구|activity|TAG|true
			festival|축제|activity|TAG|true
			bookshop|서점|activity|TAG|true
			mood|분위기|mood|GROUP|false
			healing|힐링|mood|TAG|true
			quiet|조용한|mood|TAG|true
			lively|활기찬|mood|TAG|true
			romantic|로맨틱|mood|TAG|true
			educational|교육적인|mood|TAG|true
			active|활동적인|mood|TAG|true
			unique|이색적인|mood|TAG|true
			nostalgic|레트로/향수|mood|TAG|true
			artistic|예술적인|mood|TAG|true
			open_feeling|개방감|mood|TAG|true
			modern|현대적인|mood|TAG|true
			futuristic|미래적인|mood|TAG|true
			space_context|공간/환경|space_context|GROUP|false
			outdoor|야외|space_context|TAG|true
			indoor|실내|space_context|TAG|true
			urban|도심|space_context|TAG|true
			nature_escape|도심속자연|space_context|TAG|true
			landmark|랜드마크|space_context|TAG|true
			theme_park|테마파크|space_context|TAG|true
			observatory|전망공간|space_context|TAG|true
			street_alley|거리/골목|space_context|TAG|true
			""".strip().split("\\R"))
			.map(PreferenceTagSeedIntegrationTest::toExpectedRow)
			.toList();
	}

	private static TagSeedRow toExpectedRow(String line) {
		String[] parts = line.split("\\|");
		return new TagSeedRow(
			parts[0],
			parts[1],
			parts[2],
			parts[3],
			Boolean.parseBoolean(parts[4]),
			true,
			DICTIONARY_VERSION
		);
	}

	private record TagSeedRow(
		String code,
		String displayName,
		String groupCode,
		String tagType,
		boolean selectable,
		boolean active,
		String dictionaryVersion
	) {
	}
}
