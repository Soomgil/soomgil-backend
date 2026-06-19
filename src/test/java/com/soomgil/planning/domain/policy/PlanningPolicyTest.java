package com.soomgil.planning.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.domain.model.PlanningException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlanningPolicyTest {

	@Test
	@DisplayName("DAY scope에 itineraryDayId가 없으면 PLANNING_SCOPE_DAY_MISMATCH")
	void dayScopeRequiresDayId() {
		assertThatThrownBy(() -> PlanningPolicy.validateScopeDay(PlanningScopeType.DAY, null))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_SCOPE_DAY_MISMATCH));
	}

	@Test
	@DisplayName("DAY scope에 itineraryDayId가 있으면 검증 통과")
	void dayScopeWithDayIdPasses() {
		assertThatCode(() -> PlanningPolicy.validateScopeDay(PlanningScopeType.DAY, UUID.randomUUID()))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("TRIP scope에 itineraryDayId가 있으면 PLANNING_SCOPE_DAY_MISMATCH")
	void tripScopeRejectsDayId() {
		assertThatThrownBy(() -> PlanningPolicy.validateScopeDay(PlanningScopeType.TRIP, UUID.randomUUID()))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_SCOPE_DAY_MISMATCH));
	}

	@Test
	@DisplayName("TRIP scope에 itineraryDayId가 null이면 검증 통과")
	void tripScopeWithoutDayIdPasses() {
		assertThatCode(() -> PlanningPolicy.validateScopeDay(PlanningScopeType.TRIP, null))
			.doesNotThrowAnyException();
	}
}
