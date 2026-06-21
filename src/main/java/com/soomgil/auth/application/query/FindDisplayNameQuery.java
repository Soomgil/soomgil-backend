package com.soomgil.auth.application.query;

import com.soomgil.common.cqrs.Query;
import java.util.UUID;

/**
 * 사용자 표시 이름 조회 요청.
 *
 * <p>다른 모듈(예: community)이 사용자 표시 이름이 필요할 때
 * auth mapper를 직접 호출하지 않고 이 query를 통해 조회한다.
 *
 * @param userId 사용자 식별자
 */
public record FindDisplayNameQuery(UUID userId) implements Query<String> {
}
