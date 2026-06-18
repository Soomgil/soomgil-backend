/**
 * 사용자 프로필/설정 도메인 모듈.
 *
 * <p>인증된 사용자 식별자를 기준으로 프로필({@code /me}, {@code /users/{userId}}),
 * 사용자 설정({@code /me/settings}), 계정 삭제 예약({@code DELETE /me}),
 * 사용자 검색({@code /users}) 기능을 다룬다. 세션/보안 이벤트/팔로우 관계는 각각
 * {@code auth}, {@code social} 모듈에서 담당한다.
 *
 * <p>DB 데이터는 {@code auth.users}, {@code auth.user_profiles}, {@code auth.user_settings}
 * 테이블에 저장되지만, user 도메인 개념을 다루므로 본 모듈에서 데이터 접근을 책임진다.
 * 인증 회원가입용 insert-only mapper는 {@code auth} 모듈에 그대로 둔다.
 *
 * <p>서브패키지 전체를 외부 모듈에 노출하기 위해 OPEN 모듈로 선언한다. user.api.dto 와
 * user.application.service 포트는 여러 비즈니스 모듈(auth, social, trip)에서 참조되어야
 * 하므로, 기본(closed) 모듈보다 OPEN이 이 목적에 부합한다.
 */
@org.springframework.modulith.ApplicationModule(
	type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.soomgil.user;
