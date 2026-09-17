package com.soomgil.notification.application;

import com.soomgil.notification.infrastructure.persistence.NotificationMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 PostgreSQL에서 알림 수신 범위와 세션별 중복 방지를 확인한다. */
@Testcontainers
class VoteNotificationPersistenceTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test void storesOnlyActiveParticipantsAndDeduplicatesEachEvent() throws Exception {
        var ds = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE SCHEMA auth; CREATE SCHEMA trip; CREATE SCHEMA voting;");
        jdbc.execute("CREATE TABLE auth.users(id uuid PRIMARY KEY);");
        jdbc.execute("CREATE TABLE trip.trips(id uuid PRIMARY KEY, title text, status text);");
        jdbc.execute("CREATE TABLE trip.trip_members(trip_id uuid, user_id uuid, status text);");
        jdbc.execute("CREATE TABLE voting.vote_sessions(id uuid PRIMARY KEY, trip_id uuid, created_by_user_id uuid);");
        jdbc.execute("CREATE TABLE voting.vote_session_participants(vote_session_id uuid, user_id uuid);");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V22__create_notification_schema.sql")));
        UUID trip = UUID.randomUUID(), session = UUID.randomUUID(), owner = UUID.randomUUID();
        jdbc.update("INSERT INTO trip.trips VALUES (?, '부산 여행', 'ACTIVE')", trip);
        jdbc.update("INSERT INTO voting.vote_sessions VALUES (?, ?, ?)", session, trip, owner);
        for (int i = 0; i < 4; i++) {
            UUID user = i == 0 ? owner : UUID.randomUUID();
            jdbc.update("INSERT INTO auth.users VALUES (?)", user);
            jdbc.update("INSERT INTO trip.trip_members VALUES (?, ?, ?)", trip, user, i == 2 ? "LEFT" : "ACTIVE");
            if (i < 3) jdbc.update("INSERT INTO voting.vote_session_participants VALUES (?, ?)", session, user);
        }
        var method = NotificationMapper.class.getMethod("insertVoteNotifications", UUID.class, UUID.class, String.class, String.class, Instant.class);
        String sql = String.join(" ", method.getAnnotation(Insert.class).value()).replaceAll("#\\{(\\w+)\\}", ":$1");
        var named = new NamedParameterJdbcTemplate(ds);
        for (String type : new String[]{"VOTE_STARTED", "VOTE_STARTED", "VOTE_COMPLETED", "VOTE_COMPLETED"}) {
            named.update(sql, Map.of("tripId", trip, "sessionId", session, "type", type, "title", "투표 알림", "createdAt", Timestamp.from(Instant.now())));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification.notifications", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT recipient_user_id) FROM notification.notifications", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT payload->>'route' FROM notification.notifications", String.class))
            .allMatch(route -> route.equals("/trips/" + trip + "/route?vote=1&voteSession=" + session));
    }
}
