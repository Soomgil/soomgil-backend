package com.soomgil.global.cache;

import java.lang.annotation.*;

/** 인증된 사용자별 마이페이지 조회를 짧은 TTL의 Redis 캐시에 저장한다. 인증 정보는 캐시하지 않는다. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyPageCached {
    /** 변경 시 함께 무효화할 조회 영역 이름. */
    String value();
}
