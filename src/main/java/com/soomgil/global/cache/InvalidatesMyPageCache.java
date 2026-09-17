package com.soomgil.global.cache;

import java.lang.annotation.*;

/** 성공적으로 커밋된 변경에 연결된 마이페이지 조회 영역을 무효화한다. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InvalidatesMyPageCache {
    /** 프로필, 관계, 게시물 등 영향을 받는 영역. 영역 내 사용자 캐시를 함께 무효화한다. */
    String[] value();
}
