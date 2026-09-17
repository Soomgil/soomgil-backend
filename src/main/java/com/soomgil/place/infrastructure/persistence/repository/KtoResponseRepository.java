package com.soomgil.place.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.place.infrastructure.external.KtoTourismPlaceException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 관광공사 성공 응답을 영구 재사용한다. API 키는 저장하지 않는다.
 * 동일 요청은 PostgreSQL 잠금으로 서버 간 중복 호출을 막고, 실패는 최대 1시간 후 재시도한다.
 * 외부 API 장애와 정상적인 빈 응답은 별도로 저장한다. 자동 갱신 작업은 없다.
 */
@Repository
public class KtoResponseRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    private final KtoSourceWriter writer;
    private io.micrometer.core.instrument.MeterRegistry metrics;
    @org.springframework.beans.factory.annotation.Autowired
    void configureMetrics(io.micrometer.core.instrument.MeterRegistry metrics) { this.metrics=metrics; }
    private void count(String metric, URI uri) {
        if(metrics!=null) metrics.counter("soomgil.kto."+metric,"endpoint",uri.getPath()).increment();
    }

    public KtoResponseRepository(JdbcTemplate jdbc, ObjectMapper json, PlatformTransactionManager manager,
                                KtoSourceWriter writer) {
        this.jdbc = jdbc;
        this.json = json;
        this.writer = writer;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(15);
    }

    /** 캐시가 없을 때만 loader를 호출한다. 성공한 빈 결과도 재사용한다. */
    public JsonNode load(URI uri, Supplier<JsonNode> loader) {
        String key = key(uri);
        String quotaKey=uri.getPath().substring(0,uri.getPath().lastIndexOf('/'))+"/__quota__?";
        var outcome = transaction.execute(status -> {
            var cached = refreshRequested(uri) ? null : read(key);
            if (cached != null) { count("cache.hit",uri); return new Outcome(cached, null); }
            jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", (rs,row)->true, key);
            cached = refreshRequested(uri) ? null : read(key);
            if (cached != null) { count("cache.hit",uri); return new Outcome(cached, null); }
            Boolean cooling = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM tourism_source.kto_responses WHERE request_key IN (?,?) AND retry_after>now())", Boolean.class, key, quotaKey);
            if (Boolean.TRUE.equals(cooling)) return fallback(key, new KtoTourismPlaceException("Place collection is temporarily paused after a failure."));
            JsonNode response;
            try {
                count("requests",uri);
                response = loader.get();
                validate(response);
            } catch (RuntimeException error) {
                count("failures",uri);
                jdbc.update("""
                    INSERT INTO tourism_source.kto_responses(request_key,retry_after,failure_count)
                    VALUES (?, now()+interval '5 minutes', 1)
                    ON CONFLICT(request_key) DO UPDATE SET
                      failure_count=kto_responses.failure_count+1,
                      retry_after=now()+least(60,5*power(2,least(kto_responses.failure_count,4))) * interval '1 minute'
                    """, key);
                if(isQuota(error)) jdbc.update("INSERT INTO tourism_source.kto_responses(request_key,retry_after) VALUES (?,now()+interval '1 hour') ON CONFLICT(request_key) DO UPDATE SET retry_after=excluded.retry_after",quotaKey);
                return fallback(key, error);
            }
            jdbc.update("""
                INSERT INTO tourism_source.kto_responses(request_key,payload,fetched_at,source_modified_at) VALUES (?,?::jsonb,now(),?)
                ON CONFLICT(request_key) DO UPDATE SET payload=excluded.payload,fetched_at=excluded.fetched_at,retry_after=null,failure_count=0,refresh_requested=false,source_modified_at=excluded.source_modified_at
                """, key, response.toString(), sourceModifiedAt(response));
            writer.store(uri, response);
            return new Outcome(response, null);
        });
        if (outcome.error() != null) throw outcome.error();
        return outcome.value();
    }

    /** 관리자가 표시한 재수집 요청만 확인한다. 시간 경과만으로 갱신하지 않는다. */
    public boolean refreshRequested(URI uri) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM tourism_source.kto_responses WHERE request_key=? AND refresh_requested)",Boolean.class,key(uri)));
    }
    private Outcome fallback(String key, RuntimeException error) {
        JsonNode previous=read(key);
        return previous==null?new Outcome(null,error):new Outcome(previous,null);
    }
    private boolean isQuota(Throwable error) {
        for(Throwable cause=error;cause!=null;cause=cause.getCause()) {
            if("KTO_QUOTA".equals(cause.getMessage())) return true;
            if(cause instanceof org.springframework.web.client.RestClientResponseException response && response.getStatusCode().value()==429) return true;
        }
        return false;
    }
    private java.time.OffsetDateTime sourceModifiedAt(JsonNode response) {
        java.time.OffsetDateTime latest=null;
        for(JsonNode item:response.path("response").path("body").path("items").path("item")) {
            try {
                var modified=java.time.LocalDateTime.parse(item.path("modifiedtime").asText(),java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).atOffset(java.time.ZoneOffset.ofHours(9));
                if(latest==null||modified.isAfter(latest)) latest=modified;
            } catch(java.time.format.DateTimeParseException ignored) { }
        }
        return latest;
    }

    /** 요청 파라미터 순서를 통일하고 인증키·앱 식별자를 제거한다. */
    public static String key(URI uri) {
        var parameters = Arrays.stream((uri.getRawQuery() == null ? "" : uri.getRawQuery()).split("&"))
            .filter(value -> !value.isBlank())
            .filter(value -> !List.of("servicekey", "mobileos", "mobileapp", "_type").contains(value.split("=",2)[0].toLowerCase(java.util.Locale.ROOT)))
            .sorted().toList();
        return uri.getPath() + "?" + String.join("&", parameters);
    }

    private JsonNode read(String key) {
        var values = jdbc.query("SELECT payload::text FROM tourism_source.kto_responses WHERE request_key=? AND payload IS NOT NULL",
            (rs, row) -> rs.getString(1), key);
        if (values.isEmpty()) return null;
        try { return json.readTree(values.getFirst()); }
        catch (Exception error) { throw new IllegalStateException("Stored KTO response is invalid", error); }
    }

    private void validate(JsonNode response) {
        if(response!=null && java.util.Set.of("22","LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR").contains(response.path("response").path("header").path("resultCode").asText())) throw new KtoTourismPlaceException("KTO_QUOTA");
        if (response == null || !"0000".equals(response.path("response").path("header").path("resultCode").asText())) {
            throw new KtoTourismPlaceException("KTO returned an unsuccessful response.");
        }
    }

    private record Outcome(JsonNode value, RuntimeException error) {}
}
