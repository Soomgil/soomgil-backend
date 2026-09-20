package com.soomgil.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 사용자 소유 범위를 SQL 조건으로 강제하는 알림 mapper. */
@Mapper
public interface NotificationMapper {

    /** 세션·알림 종류·사용자에서 만든 고정 ID로 중복 생성에 안전하게 저장한다. */
    @Insert("""
        INSERT INTO notification.notifications(id, recipient_user_id, actor_user_id, trip_id, type, title, body, payload, created_at)
        SELECT md5(CAST(#{sessionId} AS text) || ':' || #{type} || ':' || CAST(p.user_id AS text))::uuid,
               p.user_id, CASE WHEN #{type} = 'VOTE_STARTED' THEN s.created_by_user_id ELSE NULL END,
               s.trip_id, #{type}, #{title}, t.title || CASE WHEN #{type} = 'VOTE_STARTED'
                 THEN ' · 스티커로 가고 싶은 곳을 골라주세요.' ELSE ' · 함께 고른 여행지를 확인해보세요.' END,
               jsonb_build_object('tripId', s.trip_id, 'voteSessionId', s.id,
                 'route', '/trips/' || s.trip_id || '/route?vote=1&voteSession=' || s.id), #{createdAt}
        FROM voting.vote_session_participants p
        JOIN voting.vote_sessions s ON s.id = p.vote_session_id
        JOIN trip.trips t ON t.id = s.trip_id AND t.status = 'ACTIVE'
        JOIN trip.trip_members m ON m.trip_id = s.trip_id AND m.user_id = p.user_id AND m.status = 'ACTIVE'
        WHERE s.id = #{sessionId} AND s.trip_id = #{tripId}
        ON CONFLICT(id) DO NOTHING
        """)
    int insertVoteNotifications(@Param("tripId") UUID tripId, @Param("sessionId") UUID sessionId,
        @Param("type") String type, @Param("title") String title, @Param("createdAt") Instant createdAt);

	@Select("""
		SELECT participant.user_id
		FROM voting.vote_session_participants participant
		JOIN trip.trip_members member
		  ON member.trip_id = #{tripId}
		 AND member.user_id = participant.user_id
		 AND member.status = 'ACTIVE'
		WHERE participant.vote_session_id = #{sessionId}
		""")
	List<UUID> findVoteNotificationRecipientUserIds(@Param("tripId") UUID tripId,
		@Param("sessionId") UUID sessionId);

	@Insert("""
		INSERT INTO notification.notifications (
		  id, recipient_user_id, actor_user_id, trip_id, type, title, body, payload, created_at
		) VALUES (
		  #{id}, #{recipientUserId}, #{actorUserId}, #{tripId}, #{type}, #{title}, #{body}, #{payloadJson}::jsonb, #{createdAt}
		)
		""")
	void insert(
		@Param("id") UUID id,
		@Param("recipientUserId") UUID recipientUserId,
		@Param("actorUserId") UUID actorUserId,
		@Param("tripId") UUID tripId,
		@Param("type") String type,
		@Param("title") String title,
		@Param("body") String body,
		@Param("payloadJson") String payloadJson,
		@Param("createdAt") Instant createdAt
	);

	@Select("""
		SELECT notification.id, notification.actor_user_id,
		       profile.display_name AS actor_display_name,
		       profile.profile_image_url AS actor_profile_image_url,
		       notification.type, notification.title, notification.body,
		       notification.payload::text AS payload_json,
		       notification.read_at, notification.created_at
		FROM notification.notifications notification
		LEFT JOIN auth.user_profiles profile ON profile.user_id = notification.actor_user_id
		WHERE notification.recipient_user_id = #{recipientUserId}
		  AND (NOT #{unreadOnly} OR notification.read_at IS NULL)
		ORDER BY notification.created_at DESC, notification.id DESC
		LIMIT #{size} OFFSET #{offset}
		""")
	List<NotificationRow> findByRecipient(
		@Param("recipientUserId") UUID recipientUserId,
		@Param("unreadOnly") boolean unreadOnly,
		@Param("offset") int offset,
		@Param("size") int size
	);

	@Select("""
		SELECT COUNT(*) FROM notification.notifications
		WHERE recipient_user_id = #{recipientUserId}
		  AND (NOT #{unreadOnly} OR read_at IS NULL)
		""")
	long countByRecipient(
		@Param("recipientUserId") UUID recipientUserId,
		@Param("unreadOnly") boolean unreadOnly
	);

	@Select("""
		SELECT notification.id, notification.actor_user_id,
		       profile.display_name AS actor_display_name,
		       profile.profile_image_url AS actor_profile_image_url,
		       notification.type, notification.title, notification.body,
		       notification.payload::text AS payload_json,
		       notification.read_at, notification.created_at
		FROM notification.notifications notification
		LEFT JOIN auth.user_profiles profile ON profile.user_id = notification.actor_user_id
		WHERE notification.id = #{notificationId}
		  AND notification.recipient_user_id = #{recipientUserId}
		""")
	NotificationRow findOwned(
		@Param("notificationId") UUID notificationId,
		@Param("recipientUserId") UUID recipientUserId
	);

	@Update("""
		UPDATE notification.notifications SET read_at = COALESCE(read_at, #{readAt})
		WHERE id = #{notificationId} AND recipient_user_id = #{recipientUserId}
		""")
	int markRead(
		@Param("notificationId") UUID notificationId,
		@Param("recipientUserId") UUID recipientUserId,
		@Param("readAt") Instant readAt
	);

	@Update("""
		UPDATE notification.notifications SET read_at = #{readAt}
		WHERE recipient_user_id = #{recipientUserId} AND read_at IS NULL
		""")
	int markAllRead(@Param("recipientUserId") UUID recipientUserId, @Param("readAt") Instant readAt);

	@Delete("""
		DELETE FROM notification.notifications
		WHERE id = #{notificationId} AND recipient_user_id = #{recipientUserId}
		""")
	int deleteOwned(
		@Param("notificationId") UUID notificationId,
		@Param("recipientUserId") UUID recipientUserId
	);
}
