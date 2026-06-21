package com.soomgil.ai.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiChatMapper {

	@Insert("""
		INSERT INTO ai.ai_chat_sessions (id, trip_id, status, created_at, updated_at)
		VALUES (#{id}, #{tripId}, 'ACTIVE', #{createdAt}, #{createdAt})
		ON CONFLICT (trip_id) DO NOTHING
		""")
	void insertSessionIfAbsent(@Param("id") UUID id, @Param("tripId") UUID tripId, @Param("createdAt") Instant createdAt);

	@Select("""
		SELECT id, trip_id, status, summary, summary_updated_at, created_at
		FROM ai.ai_chat_sessions
		WHERE trip_id = #{tripId} AND deleted_at IS NULL
		""")
	AiChatSessionRow findSessionByTripId(@Param("tripId") UUID tripId);

	@Insert("""
		INSERT INTO ai.ai_chat_messages (id, session_id, requester_user_id, role, content, created_at)
		VALUES (#{id}, #{sessionId}, #{requesterUserId}, #{role}, #{content}, #{createdAt})
		""")
	void insertMessage(
		@Param("id") UUID id,
		@Param("sessionId") UUID sessionId,
		@Param("requesterUserId") UUID requesterUserId,
		@Param("role") String role,
		@Param("content") String content,
		@Param("createdAt") Instant createdAt
	);

	@Select("""
		SELECT m.id, m.session_id, m.requester_user_id, m.role, m.content, m.tool_call_id, m.created_at,
		       p.display_name AS requester_display_name, p.profile_image_url AS requester_profile_image_url
		FROM ai.ai_chat_messages m
		LEFT JOIN auth.user_profiles p ON p.user_id = m.requester_user_id
		WHERE m.session_id = #{sessionId}
		ORDER BY m.created_at DESC, m.id DESC
		LIMIT #{limit} OFFSET #{offset}
		""")
	List<AiChatMessageRow> findMessages(
		@Param("sessionId") UUID sessionId,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Select("""
		SELECT m.id, m.session_id, m.requester_user_id, m.role, m.content, m.tool_call_id, m.created_at,
		       p.display_name AS requester_display_name, p.profile_image_url AS requester_profile_image_url
		FROM ai.ai_chat_messages m
		LEFT JOIN auth.user_profiles p ON p.user_id = m.requester_user_id
		WHERE m.id = #{id}
		""")
	AiChatMessageRow findMessageById(@Param("id") UUID id);

	@Select("""
		SELECT id, session_id, requester_user_id, role, content, tool_call_id, created_at,
		       NULL AS requester_display_name, NULL AS requester_profile_image_url
		FROM ai.ai_chat_messages
		WHERE session_id = #{sessionId}
		ORDER BY created_at DESC, id DESC
		LIMIT #{limit}
		""")
	List<AiChatMessageRow> findRecentMessages(@Param("sessionId") UUID sessionId, @Param("limit") int limit);
}
