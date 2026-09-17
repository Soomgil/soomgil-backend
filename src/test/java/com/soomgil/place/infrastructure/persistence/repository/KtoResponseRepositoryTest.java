package com.soomgil.place.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.soomgil.place.infrastructure.external.KtoTourismPlaceException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class KtoResponseRepositoryTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
    JdbcTemplate jdbc;
    KtoResponseRepository store;
    ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    @BeforeEach void prepare() throws Exception {
        var ds=new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
        jdbc=new JdbcTemplate(ds);
        jdbc.execute("DROP SCHEMA IF EXISTS tourism_source CASCADE");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V24__create_tourism_source_place_tables.sql")));
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V52__persist_kto_responses.sql")));
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V53__track_kto_manual_refresh.sql")));
        store=new KtoResponseRepository(jdbc,json,new DataSourceTransactionManager(ds),new KtoSourceWriter(jdbc));
    }
    JsonNode response(String items) throws Exception {
        return json.readTree("{\"response\":{\"header\":{\"resultCode\":\"0000\"},\"body\":{\"items\":{\"item\":"+items+"}}}}");
    }
    @Test void savesOnceAndIndexesPlacesWithoutStoringCredentials() throws Exception {
        URI uri=URI.create("https://example.org/B551011/KorService2/areaBasedList2?serviceKey=SECRET&pageNo=1");
        var payload=response("[{\"contentid\":\"100\",\"title\":\"바다\",\"contenttypeid\":\"12\",\"firstimage\":\"https://example.org/a.jpg\"}]");
        AtomicInteger calls=new AtomicInteger();
        store.load(uri,()->{calls.incrementAndGet();return payload;});
        store.load(uri,()->{calls.incrementAndGet();return payload;});
        assertThat(calls.get()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tourism_source.attractions",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tourism_source.attraction_images",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT request_key FROM tourism_source.kto_responses",String.class)).doesNotContain("SECRET","serviceKey");
    }
    @Test void sharesConcurrentCallsAndCachesSuccessfulEmptyResults() throws Exception {
        URI uri=URI.create("https://example.org/B551011/KorService2/detailImage2?contentId=100");
        var payload=response("[]"); AtomicInteger calls=new AtomicInteger();
        try(var executor=Executors.newFixedThreadPool(4)) {
            var tasks=new java.util.ArrayList<Future<JsonNode>>();
            for(int i=0;i<4;i++)tasks.add(executor.submit(()->store.load(uri,()->{calls.incrementAndGet();try{Thread.sleep(100);}catch(InterruptedException e){Thread.currentThread().interrupt();}return payload;})));
            for(var task:tasks)assertThat(task.get(10,TimeUnit.SECONDS)).isEqualTo(payload);
        }
        assertThat(calls.get()).isEqualTo(1);
    }
    @Test void failuresDoNotBecomeEmptySuccessAndBlockImmediateRetries() {
        URI uri=URI.create("https://example.org/B551011/KorService2/detailCommon2?contentId=100");
        AtomicInteger calls=new AtomicInteger();
        assertThatThrownBy(()->store.load(uri,()->{calls.incrementAndGet();throw new KtoTourismPlaceException("quota");})).isInstanceOf(KtoTourismPlaceException.class);
        assertThatThrownBy(()->store.load(uri,()->{calls.incrementAndGet();return null;})).isInstanceOf(KtoTourismPlaceException.class);
        assertThat(calls.get()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT payload IS NULL AND retry_after>now() FROM tourism_source.kto_responses",Boolean.class)).isTrue();
    }
    @Test void failedManualRefreshPreservesGoodDataAndQuotaStopsOtherMisses() throws Exception {
        URI uri=URI.create("https://example.org/B551011/KorService2/detailCommon2?contentId=100");
        var payload=response("[]");
        store.load(uri,()->payload);
        jdbc.update("UPDATE tourism_source.kto_responses SET refresh_requested=true");
        assertThat(store.load(uri,()->{throw new KtoTourismPlaceException("KTO_QUOTA");})).isEqualTo(payload);
        AtomicInteger calls=new AtomicInteger();
        URI other=URI.create("https://example.org/B551011/KorService2/detailCommon2?contentId=200");
        assertThatThrownBy(()->store.load(other,()->{calls.incrementAndGet();return payload;})).isInstanceOf(KtoTourismPlaceException.class);
        assertThat(calls.get()).isZero();
        assertThat(jdbc.queryForObject("SELECT payload::text FROM tourism_source.kto_responses WHERE request_key=?",String.class,KtoResponseRepository.key(uri))).isNotNull();
    }
    @Test void databaseCandidatesAreFilteredBeforeCollectionAndOlderDataCannotReplaceNewerData() throws Exception {
        var first=URI.create("https://example.org/B551011/KorService2/areaBasedList2?pageNo=1");
        var second=URI.create("https://example.org/B551011/KorService2/areaBasedList2?pageNo=2");
        var recent=response("[{\"contentid\":\"100\",\"title\":\"최신\",\"contenttypeid\":\"12\",\"modifiedtime\":\"20260917000000\"}]");
        var older=response("[{\"contentid\":\"100\",\"title\":\"과거\",\"modifiedtime\":\"20250917000000\"}]");
        store.load(first,()->recent);store.load(second,()->older);
        var places=new KtoStoredPlaces(new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc));
        var request=new com.soomgil.place.application.port.TourismPlaceLiveSearchRequest(null,null,null,null,10);
        assertThat(places.search(request,java.util.List.of(),"")).extracting(com.soomgil.place.application.port.TourismPlaceFeedItem::name).containsExactly("최신");
        assertThat(places.search(request,java.util.List.of("100"),"")).isEmpty();
    }
}
