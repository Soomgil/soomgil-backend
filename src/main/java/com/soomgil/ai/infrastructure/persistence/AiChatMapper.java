package com.soomgil.ai.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

	@Insert("""
		INSERT INTO ai.ai_tool_calls (
		  id, session_id, trip_id, request_message_id, requested_by_user_id,
		  websocket_session_id, tool_name, execution_policy, arguments, status, version_before, created_at
		) VALUES (
		  #{id}, #{sessionId}, #{tripId}, #{requestMessageId}, #{userId},
		  #{websocketSessionId}, #{toolName}, #{executionPolicy}, #{argumentsJson}::jsonb, 'REQUESTED', #{versionBefore}, #{createdAt}
		)
		""")
	void insertToolCall(
		@Param("id") UUID id, @Param("sessionId") UUID sessionId, @Param("tripId") UUID tripId,
		@Param("requestMessageId") UUID requestMessageId, @Param("userId") UUID userId,
		@Param("websocketSessionId") String websocketSessionId,
		@Param("toolName") String toolName, @Param("executionPolicy") String executionPolicy,
		@Param("argumentsJson") String argumentsJson, @Param("versionBefore") Long versionBefore,
		@Param("createdAt") Instant createdAt
	);

	@Update("""
		UPDATE ai.ai_tool_calls SET result = #{resultJson}::jsonb, status = 'SUCCEEDED',
		       version_after = #{versionAfter}, undo_redo_available = #{undoAvailable}, completed_at = #{completedAt}
		WHERE id = #{id}
		""")
	void completeToolCall(
		@Param("id") UUID id, @Param("resultJson") String resultJson,
		@Param("versionAfter") Long versionAfter, @Param("undoAvailable") boolean undoAvailable,
		@Param("completedAt") Instant completedAt
	);

	@Update("""
		UPDATE ai.ai_tool_calls SET status = 'FAILED', error_code = #{errorCode},
		       error_message = #{errorMessage}, completed_at = #{completedAt}
		WHERE id = #{id}
		""")
	void failToolCall(
		@Param("id") UUID id, @Param("errorCode") String errorCode,
		@Param("errorMessage") String errorMessage, @Param("completedAt") Instant completedAt
	);

	@Update("UPDATE ai.ai_tool_calls SET result_message_id = #{messageId} WHERE id = #{toolCallId}")
	void linkToolCallToResultMessage(@Param("toolCallId") UUID toolCallId, @Param("messageId") UUID messageId);
}
