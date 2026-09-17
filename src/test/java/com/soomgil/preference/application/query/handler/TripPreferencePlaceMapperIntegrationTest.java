package com.soomgil.preference.application.query.handler;

import com.soomgil.preference.infrastructure.persistence.mapper.TripPreferencePlaceMapper;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** 임시 PostgreSQL 전용: 실제 MyBatis 매핑과 공개 범위, 지도 범위, 긍정 반응 필터를 검증한다. */
@EnabledIfEnvironmentVariable(named = "TASTE_TEST_PORT", matches = "\\d+")
class TripPreferencePlaceMapperIntegrationTest {
    @Test
    void returnsOnlyAllowedMembersAndPositiveReactionsInViewport() throws Exception {
        String url = "jdbc:postgresql://localhost:" + System.getenv("TASTE_TEST_PORT") + "/postgres";
        var ds = new UnpooledDataSource("org.postgresql.Driver", url, "postgres", "taste-qa");
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        config.setMapUnderscoreToCamelCase(true);
        String resource = "mappers/preference/TripPreferencePlaceMapper.xml";
        try (var xml = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(xml, config, resource, config.getSqlFragments()).parse();
        }
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            var connection = session.getConnection();
            try (var s = connection.createStatement()) {
                s.execute("""
                    CREATE SCHEMA auth; CREATE SCHEMA preference; CREATE SCHEMA tourism_source; CREATE SCHEMA social;
                    CREATE TABLE auth.user_profiles(user_id uuid, display_name text, profile_image_url text, profile_visibility text);
                    CREATE TABLE social.user_follows(follower_user_id uuid, following_user_id uuid, status text, deleted_at timestamptz);
                    CREATE TABLE preference.user_place_reactions(user_id uuid, provider text, external_place_id text, reaction text);
                    CREATE TABLE tourism_source.attractions(no bigint, content_id bigint, title text, addr1 text, addr2 text, latitude double precision, longitude double precision, content_type_id int);
                    CREATE TABLE tourism_source.contenttypes(content_type_id int, content_type_name text);
                    CREATE TABLE tourism_source.attraction_images(attraction_no bigint, public_url text, is_active boolean, display_order int, created_at timestamptz);
                    INSERT INTO auth.user_profiles SELECT ('00000000-0000-0000-0000-' || lpad(i::text,12,'0'))::uuid, 'Member '||i, NULL,
                      CASE WHEN i IN (1,3,4,5) THEN 'PRIVATE' ELSE 'PUBLIC' END FROM generate_series(1,6) i;
                    INSERT INTO tourism_source.attractions VALUES (1,1,'Inside','A','B',33.5,126.5,1),(2,2,'Outside',NULL,NULL,38,128,1);
                    INSERT INTO preference.user_place_reactions SELECT user_id,'KTO','1',CASE WHEN display_name='Member 6' THEN 'NOPE' ELSE 'LIKE' END FROM auth.user_profiles;
                    INSERT INTO preference.user_place_reactions SELECT user_id,'KTO','2','SUPER_LIKE' FROM auth.user_profiles;
                    INSERT INTO social.user_follows VALUES ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000004','ACTIVE',NULL),
                      ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000005','PENDING',NULL);
                    """);
            }
            var ids = java.util.stream.IntStream.rangeClosed(1,6).mapToObj(i -> "00000000-0000-0000-0000-" + String.format("%012d", i)).toList();
            var mapper = session.getMapper(TripPreferencePlaceMapper.class);
            var result = mapper.find(ids.get(0), ids, 126,33,127,34);
            assertEquals(List.of(ids.get(0),ids.get(1),ids.get(3)), result.stream().map(r -> r.userId()).toList());
            assertTrue(result.stream().allMatch(r -> r.name().equals("Inside") && r.reaction().equals("LIKE")));
            assertEquals(33.5, result.get(0).lat());
            assertEquals(1, mapper.find(ids.get(0), List.of(ids.get(0)),126,33,127,34).size());
            assertTrue(mapper.find(ids.get(0),ids,120,30,121,31).isEmpty());
            session.rollback();
        }
    }
}
