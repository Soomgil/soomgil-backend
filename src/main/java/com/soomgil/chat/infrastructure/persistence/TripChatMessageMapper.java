package com.soomgil.chat.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 여행방 채팅 메시지 저장과 최신순 조회 mapper. */
@Mapper
public interface TripChatMessageMapper {

	@Select("""
		SELECT message.id, message.trip_id, message.sender_user_id,
		       profile.display_name AS sender_display_name,
		       profile.profile_image_url AS sender_profile_image_url,
		       message.content, message.deleted_at, message.created_at
		FROM chat.trip_chat_messages message
		JOIN auth.user_profiles profile ON profile.user_id = message.sender_user_id
		WHERE message.trip_id = #{tripId}
		ORDER BY message.created_at DESC, message.id DESC
		LIMIT #{limit} OFFSET #{offset}
		""")
	List<TripChatMessageRow> findByTrip(
		@Param("tripId") UUID tripId,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Select("""
		SELECT message.id, message.trip_id, message.sender_user_id,
		       profile.display_name AS sender_display_name,
		       profile.profile_image_url AS sender_profile_image_url,
		       message.content, message.deleted_at, message.created_at
		FROM chat.trip_chat_messages message
		JOIN auth.user_profiles profile ON profile.user_id = message.sender_user_id
		WHERE message.id = #{messageId}
		""")
	TripChatMessageRow findById(@Param("messageId") UUID messageId);

	@Insert("""
		INSERT INTO chat.trip_chat_messages (id, trip_id, sender_user_id, content, created_at)
		VALUES (#{id}, #{tripId}, #{senderUserId}, #{content}, #{createdAt})
		""")
	void insert(
		@Param("id") UUID id,
		@Param("tripId") UUID tripId,
		@Param("senderUserId") UUID senderUserId,
		@Param("content") String content,
		@Param("createdAt") Instant createdAt
	);

	@Update("""
		UPDATE chat.trip_chat_messages
		SET deleted_by_user_id = #{senderUserId}, deleted_at = #{deletedAt}
		WHERE id = #{messageId} AND trip_id = #{tripId}
		  AND sender_user_id = #{senderUserId} AND deleted_at IS NULL
		""")
	int softDelete(
		@Param("tripId") UUID tripId,
		@Param("messageId") UUID messageId,
		@Param("senderUserId") UUID senderUserId,
		@Param("deletedAt") Instant deletedAt
	);
}
