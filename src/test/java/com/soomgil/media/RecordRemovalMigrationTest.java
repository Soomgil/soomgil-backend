package com.soomgil.media;

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RecordRemovalMigrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void removesRecordsAndQueuesOnlyExclusivePhotosForStorageDeletion() throws Exception {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .target("50").load().migrate();
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var sql = connection.createStatement()) {
            sql.execute("INSERT INTO auth.users(id) VALUES ('10000000-0000-0000-0000-000000000001')");
            sql.execute("INSERT INTO trip.trips(id, owner_user_id, title, created_at, updated_at) VALUES ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '여행', now(), now())");
            sql.execute("INSERT INTO record.trip_record_entries(id, trip_id, uploaded_by_user_id, created_at, updated_at) VALUES ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', now(), now())");
            for (int i = 1; i <= 3; i++) {
                String id = "40000000-0000-0000-0000-00000000000" + i;
                sql.execute("INSERT INTO media.media_files(id, bucket, object_key, created_at) VALUES ('" + id + "', 'test', 'media/user/trip-record/" + i + ".jpg', now())");
                sql.execute("INSERT INTO record.trip_record_media(record_entry_id, media_file_id, created_at) VALUES ('30000000-0000-0000-0000-000000000001', '" + id + "', now())");
            }
            // 이미 커뮤니티에 게시한 사진은 공용 참조로 유지한다.
            sql.execute("INSERT INTO community.threads(id, author_user_id, content, created_at, updated_at) VALUES ('50000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '여행기', now(), now())");
            sql.execute("INSERT INTO community.thread_media(thread_id, media_file_id, created_at) VALUES ('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', now())");
            // 다른 리소스에 명시적으로 연결한 사진도 유지한다.
            sql.execute("UPDATE media.media_files SET linked_resource_type='TRIP', linked_resource_id='20000000-0000-0000-0000-000000000001' WHERE id='40000000-0000-0000-0000-000000000003'");
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()).load().migrate();
            try (var result = sql.executeQuery("SELECT count(*) FROM information_schema.schemata WHERE schema_name='record'")) {
                result.next(); assertThat(result.getInt(1)).isZero();
            }
            try (var result = sql.executeQuery("SELECT status, purge_after_at <= now() AS due FROM media.media_files ORDER BY id")) {
                result.next(); assertThat(result.getString("status")).isEqualTo("DELETED"); assertThat(result.getBoolean("due")).isTrue();
                result.next(); assertThat(result.getString("status")).isEqualTo("ACTIVE");
                result.next(); assertThat(result.getString("status")).isEqualTo("ACTIVE");
            }
        }
    }
}
