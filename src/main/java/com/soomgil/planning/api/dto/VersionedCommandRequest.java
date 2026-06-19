package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * version 검증이 필요한 delete/mutation 요청의 공통 본문.
 *
 * <p>planning 도메인은 낙관적 동시성 제어를 사용한다. 클라이언트는 마지막으로 읽은
 * 리소스의 {@code version}을 {@code baseVersion}으로 보내고, 서버는 UPDATE 시
 * {@code WHERE version = ?}로 일치하는지 검증한다. 불일치 시
 * {@link com.soomgil.global.error.ErrorCode#PLANNING_VERSION_CONFLICT}를 던진다.
 *
 * <p>collaboration 모듈이 아직 scaffold 단계라 동일한 형태의 record를 planning 내부에
 * 따로 둔다. collaboration이 구축된 뒤 통합 여부는 별도 검토.
 *
 * @param baseVersion 클라이언트가 마지막으로 읽은 리소스 version
 */
public record VersionedCommandRequest(
	@NotNull
	Long baseVersion
) {
}
