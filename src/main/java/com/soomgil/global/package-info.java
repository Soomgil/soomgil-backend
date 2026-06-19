/**
 * 전역 인프라 모듈.
 *
 * <p>보안(JWT, OAuth2 자원 서버, {@code CurrentUser} 주입), 에러 처리(
 * {@code BusinessException}, {@code ErrorCode}, {@code ProblemDetails} 팩토리),
 * Spring 설정, 애플리케이션 이벤트, 영속성 인프라, 외부 연동, 저장소, 웹 횡단 관심사 등
 * 애플리케이션 전반에 걸친 기반 기능을 다룬다.
 *
 * <p>서브패키지 전체를 외부 모듈에 노출하기 위해 OPEN 모듈로 선언한다. 보안 주체와 에러 타입은
 * 여러 비즈니스 모듈에서 자유롭게 참조되어야 하므로, 기본(closed) 모듈보다 OPEN이 이 목적에
 * 부합한다.
 */
@org.springframework.modulith.ApplicationModule(
	type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.soomgil.global;
