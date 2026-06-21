/**
 * 공통 기반 모듈.
 *
 * <p>모든 애플리케이션 모듈이 공유하는 API 지원({@code ApiControllerSupport}), CQRS 기반
 * ({@code Command}/{@code Query} 및 handler), 페이지네이션 응답, 식별자 생성, 시간 제공,
 * 검증 규칙 등을 제공한다.
 *
 * <p>서브패키지 전체를 외부 모듈에 노출하기 위해 OPEN 모듈로 선언한다. 공유 기반 코드는
 * 여러 비즈니스 모듈에서 자유롭게 참조되어야 하므로, 기본(closed) 모듈보다 OPEN이 이 목적에
 * 부합한다.
 */
@org.springframework.modulith.ApplicationModule(
	type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.soomgil.common;
