package com.soomgil.tourismsource.imports;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 관광 원천 import manifest.
 *
 * @param manifestVersion manifest format version
 * @param sourceProvider 원천 provider
 * @param sourceSnapshot 원천 snapshot 이름
 * @param generatedAt manifest 생성 시각
 * @param datasets import dataset 목록
 */
public record TourismSourceImportManifest(
	String manifestVersion,
	String sourceProvider,
	String sourceSnapshot,
	OffsetDateTime generatedAt,
	List<TourismSourceImportDataset> datasets
) {

	public TourismSourceImportManifest {
		datasets = datasets == null ? List.of() : List.copyOf(datasets);
	}

	/**
	 * manifest가 원천 import 정책을 만족하는지 검증한다.
	 */
	public void validate() {
		requireText(manifestVersion, "manifestVersion is required.");
		requireText(sourceProvider, "sourceProvider is required.");
		requireText(sourceSnapshot, "sourceSnapshot is required.");
		if (generatedAt == null) {
			throw new IllegalArgumentException("generatedAt is required.");
		}
		if (datasets.isEmpty()) {
			throw new IllegalArgumentException("datasets must not be empty.");
		}

		Set<String> datasetNames = new HashSet<>();
		Set<Integer> loadOrders = new HashSet<>();
		for (TourismSourceImportDataset dataset : datasets) {
			dataset.validate();
			if (!datasetNames.add(dataset.dataset())) {
				throw new IllegalArgumentException("duplicate dataset: " + dataset.dataset());
			}
			if (!loadOrders.add(dataset.loadOrder())) {
				throw new IllegalArgumentException("duplicate loadOrder: " + dataset.loadOrder());
			}
		}
	}

	/**
	 * import 순서대로 정렬한 dataset 목록을 반환한다.
	 *
	 * @return loadOrder 오름차순 dataset 목록
	 */
	public List<TourismSourceImportDataset> datasetsInLoadOrder() {
		return datasets.stream()
			.sorted(Comparator.comparingInt(TourismSourceImportDataset::loadOrder))
			.toList();
	}

	private void requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
	}
}
