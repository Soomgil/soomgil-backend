package com.soomgil.tourismsource.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TourismSourceImportManifestLoaderTest {

	private final TourismSourceImportManifestLoader loader = new TourismSourceImportManifestLoader();

	@Test
	void loadsBundledKtoSsfayImportManifest() {
		TourismSourceImportManifest manifest = loader.load(
			"tourism-source/imports/kto-ssafy-v1.import-manifest.json"
		);

		assertThat(manifest.manifestVersion()).isEqualTo("tourism-source-import-manifest-v1");
		assertThat(manifest.sourceProvider()).isEqualTo("KTO");
		assertThat(manifest.datasetsInLoadOrder())
			.extracting(TourismSourceImportDataset::dataset)
			.containsExactly("sidos", "guguns", "contenttypes", "attractions", "attraction_images");
		assertThat(manifest.datasetsInLoadOrder())
			.extracting(TourismSourceImportDataset::targetTable)
			.allSatisfy(targetTable -> assertThat(targetTable).startsWith("tourism_source."));
	}

	@Test
	void rejectsDuplicateDatasetNames() {
		TourismSourceImportManifest manifest = new TourismSourceImportManifest(
			"tourism-source-import-manifest-v1",
			"KTO",
			"test-snapshot",
			OffsetDateTime.parse("2026-06-19T00:00:00Z"),
			List.of(
				dataset("attractions", "tourism_source.attractions", 10),
				dataset("attractions", "tourism_source.attractions", 20)
			)
		);

		assertThatThrownBy(manifest::validate)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("duplicate dataset");
	}

	@Test
	void rejectsLocalRawSourceLocations() {
		TourismSourceImportManifest manifest = new TourismSourceImportManifest(
			"tourism-source-import-manifest-v1",
			"KTO",
			"test-snapshot",
			OffsetDateTime.parse("2026-06-19T00:00:00Z"),
			List.of(dataset("attractions", "tourism_source.attractions", 10, "classpath:raw/attractions.csv"))
		);

		assertThatThrownBy(manifest::validate)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("sourceLocation must be external");
	}

	@Test
	void rejectsNonTourismSourceTargetTables() {
		TourismSourceImportManifest manifest = new TourismSourceImportManifest(
			"tourism-source-import-manifest-v1",
			"KTO",
			"test-snapshot",
			OffsetDateTime.parse("2026-06-19T00:00:00Z"),
			List.of(dataset("attractions", "place.attractions", 10))
		);

		assertThatThrownBy(manifest::validate)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("targetTable must start with tourism_source.");
	}

	private TourismSourceImportDataset dataset(String dataset, String targetTable, int loadOrder) {
		return dataset(dataset, targetTable, loadOrder, "external://kto-ssafy/" + dataset + ".csv");
	}

	private TourismSourceImportDataset dataset(
		String dataset,
		String targetTable,
		int loadOrder,
		String sourceLocation
	) {
		return new TourismSourceImportDataset(
			dataset,
			targetTable,
			sourceLocation,
			"CSV",
			null,
			loadOrder,
			true
		);
	}
}
