package com.soomgil.tourismsource.imports;

/**
 * 관광 원천 import manifest의 단일 dataset 정의.
 *
 * @param dataset dataset 이름
 * @param targetTable 적재 대상 tourism_source table
 * @param sourceLocation 원천 파일 외부 위치
 * @param format 원천 파일 format
 * @param checksumSha256 원천 파일 checksum, 아직 고정 전이면 {@code null}
 * @param loadOrder import 순서
 * @param required 필수 dataset 여부
 */
public record TourismSourceImportDataset(
	String dataset,
	String targetTable,
	String sourceLocation,
	String format,
	String checksumSha256,
	int loadOrder,
	boolean required
) {

	/**
	 * dataset 정의가 import manifest 정책을 만족하는지 검증한다.
	 */
	public void validate() {
		requireText(dataset, "dataset is required.");
		requireText(targetTable, "targetTable is required.");
		requireText(sourceLocation, "sourceLocation is required.");
		requireText(format, "format is required.");

		if (!targetTable.startsWith("tourism_source.")) {
			throw new IllegalArgumentException("targetTable must start with tourism_source.");
		}
		if (!isExternalSourceLocation(sourceLocation)) {
			throw new IllegalArgumentException("sourceLocation must be external.");
		}
		if (loadOrder <= 0) {
			throw new IllegalArgumentException("loadOrder must be positive.");
		}
		if (checksumSha256 != null && !checksumSha256.isBlank() && !checksumSha256.matches("[0-9a-fA-F]{64}")) {
			throw new IllegalArgumentException("checksumSha256 must be a 64 character hex string.");
		}
	}

	private boolean isExternalSourceLocation(String value) {
		return value.startsWith("external://")
			|| value.startsWith("s3://")
			|| value.startsWith("minio://")
			|| value.startsWith("https://");
	}

	private void requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
	}
}
