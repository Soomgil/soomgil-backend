package com.soomgil.social.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.social.api.dto.FollowStatus;
import com.soomgil.social.application.port.SocialFollowRepository;
import com.soomgil.social.domain.model.SocialFollowRecord;
import com.soomgil.social.domain.model.SocialFollowRequestRecord;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SocialFollowServiceTest {

	private static final UUID CURRENT = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID TARGET = UUID.fromString("10000000-0000-0000-0000-000000000002");
	private static final Instant NOW = Instant.parse("2026-06-20T12:00:00Z");
	private final FakeRepository repository = new FakeRepository();
	private final TimeProvider time = () -> NOW;
	private final SocialFollowService service = new SocialFollowService(repository, time);

	@Test
	void followsPublicProfileImmediatelyAndPrivateProfileAsRequest() {
		repository.visibility = "PUBLIC";
		assertThat(service.follow(CURRENT, TARGET).status()).isEqualTo(FollowStatus.ACTIVE);

		repository.visibility = "PRIVATE";
		assertThat(service.follow(CURRENT, TARGET).status()).isEqualTo(FollowStatus.PENDING);
	}

	@Test
	void rejectsSelfFollow() {
		assertThatThrownBy(() -> service.follow(CURRENT, CURRENT))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.CANNOT_FOLLOW_SELF));
	}

	@Test
	void acceptsAndRejectsPendingRequestForCurrentUser() {
		repository.follow = new SocialFollowRecord(TARGET, CURRENT, "PENDING", NOW, NOW, null);

		assertThat(service.accept(CURRENT, TARGET).status()).isEqualTo(FollowStatus.ACTIVE);

		repository.follow = new SocialFollowRecord(TARGET, CURRENT, "PENDING", NOW, NOW, null);
		service.reject(CURRENT, TARGET);
		assertThat(repository.follow.status()).isEqualTo("DELETED");
	}

	@Test
	void returnsPendingRequestsWithPageMetadata() {
		repository.requests = List.of(new SocialFollowRequestRecord(
			TARGET, "Follower", null, NOW
		));
		repository.total = 1;

		var result = service.listPending(CURRENT, 0, 20);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().follower().displayName()).isEqualTo("Follower");
		assertThat(result.page().totalElements()).isEqualTo(1);
	}

	private static final class FakeRepository implements SocialFollowRepository {
		private String visibility;
		private SocialFollowRecord follow;
		private List<SocialFollowRequestRecord> requests = List.of();
		private long total;

		@Override
		public String findProfileVisibility(UUID userId) {
			return visibility;
		}

		@Override
		public SocialFollowRecord upsert(UUID followerId, UUID followingId, String status, Instant now) {
			follow = new SocialFollowRecord(followerId, followingId, status, now, now, null);
			return follow;
		}

		@Override
		public SocialFollowRecord find(UUID followerId, UUID followingId) {
			return follow;
		}

		@Override
		public boolean activatePending(UUID followerId, UUID followingId, Instant now) {
			if (follow == null || !"PENDING".equals(follow.status())) return false;
			follow = new SocialFollowRecord(followerId, followingId, "ACTIVE", follow.createdAt(), now, null);
			return true;
		}

		@Override
		public boolean delete(UUID followerId, UUID followingId, String requiredStatus, Instant now) {
			if (follow == null || (requiredStatus != null && !requiredStatus.equals(follow.status()))) return false;
			follow = new SocialFollowRecord(followerId, followingId, "DELETED", follow.createdAt(), now, now);
			return true;
		}

		@Override
		public List<SocialFollowRequestRecord> findPending(UUID followingId, int offset, int size) {
			return requests;
		}

		@Override
		public long countPending(UUID followingId) {
			return total;
		}
	}
}
