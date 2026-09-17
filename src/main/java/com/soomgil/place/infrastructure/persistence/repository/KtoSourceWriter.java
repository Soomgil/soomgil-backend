package com.soomgil.place.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 수집한 KTO 목록·공통 정보·사진을 검색 가능한 기존 원천 테이블에 반영한다. */
@Repository
public class KtoSourceWriter {
    private final JdbcTemplate jdbc;
    private static final Map<Integer,String> TYPES = Map.of(12,"관광지",14,"문화시설",15,"축제·공연·행사",25,"여행코스",28,"레포츠",32,"숙박",38,"쇼핑",39,"음식점");
    public KtoSourceWriter(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    /** 원천 수정 시각이 오래된 결과와 빈 필드는 기존의 정상 값을 지우지 않는다. */
    public void store(URI uri, JsonNode response) {
        JsonNode items=response.path("response").path("body").path("items").path("item");
        if (!items.isArray()) return;
        for (JsonNode item:items) {
            Integer id=integer(item,"contentid");
            if (id==null) id=parameterId(uri);
            if (id==null) continue;
            jdbc.query("SELECT pg_advisory_xact_lock(?)", (rs,row)->true, id.longValue());
            String title=text(item,"title");
            if (title!=null) {
                Integer type=integer(item,"contenttypeid");
                if (type!=null) jdbc.update("INSERT INTO tourism_source.contenttypes VALUES (?,?) ON CONFLICT DO NOTHING",type,TYPES.getOrDefault(type,"기타"));
                jdbc.update("""
                    INSERT INTO tourism_source.attractions(content_id,title,content_type_id,area_code,si_gun_gu_code,
                      first_image1,first_image2,latitude,longitude,tel,addr1,addr2,overview,source_modified_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(content_id) DO UPDATE SET
                      title=excluded.title,
                      content_type_id=coalesce(excluded.content_type_id,attractions.content_type_id),
                      area_code=coalesce(excluded.area_code,attractions.area_code),si_gun_gu_code=coalesce(excluded.si_gun_gu_code,attractions.si_gun_gu_code),
                      first_image1=coalesce(excluded.first_image1,attractions.first_image1),first_image2=coalesce(excluded.first_image2,attractions.first_image2),
                      latitude=coalesce(excluded.latitude,attractions.latitude),longitude=coalesce(excluded.longitude,attractions.longitude),
                      tel=coalesce(excluded.tel,attractions.tel),addr1=coalesce(excluded.addr1,attractions.addr1),addr2=coalesce(excluded.addr2,attractions.addr2),
                      overview=coalesce(excluded.overview,attractions.overview),
                      source_modified_at=coalesce(excluded.source_modified_at,attractions.source_modified_at),imported_at=now()
                    WHERE attractions.source_modified_at IS NULL OR excluded.source_modified_at>=attractions.source_modified_at
                    """,id,title,type,integer(item,"areacode"),integer(item,"sigungucode"),text(item,"firstimage"),text(item,"firstimage2"),
                    number(item,"mapy"),number(item,"mapx"),text(item,"tel"),text(item,"addr1"),text(item,"addr2"),text(item,"overview"),modified(text(item,"modifiedtime")));
                // 수정 시각이 없는 응답도 누락된 소개만 보완할 수 있다.
                jdbc.update("UPDATE tourism_source.attractions SET overview=? WHERE content_id=? AND nullif(overview,'') IS NULL",text(item,"overview"),id);
            }
            String image=text(item,"originimgurl");
            if (image != null) {
                String license=text(item,"cpyrhtDivCd");
                if (!("Type1".equalsIgnoreCase(license) || "Type3".equalsIgnoreCase(license))) continue;
            }
            if (image==null) image=text(item,"firstimage");
            if (image!=null) {
                jdbc.update("""
                    INSERT INTO tourism_source.attraction_images(id,attraction_no,source_provider,source_type,original_url,public_url,display_order)
                    SELECT ?,a.no,'KTO','TOUR_API',?,?,100 FROM tourism_source.attractions a
                    WHERE a.content_id=? AND NOT EXISTS(SELECT 1 FROM tourism_source.attraction_images i WHERE i.attraction_no=a.no AND i.public_url=?)
                    """,UUID.randomUUID(),image,image,id,image);
            }
        }
    }
    private static String text(JsonNode n,String key) { String v=n.path(key).asText("").strip();return v.isEmpty()?null:v; }
    private static Integer integer(JsonNode n,String key) { try{return Integer.valueOf(text(n,key));}catch(Exception e){return null;} }
    private static Double number(JsonNode n,String key) { try{return Double.valueOf(text(n,key));}catch(Exception e){return null;} }
    private static OffsetDateTime modified(String value) { try{return LocalDateTime.parse(value,DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).atOffset(ZoneOffset.ofHours(9));}catch(Exception e){return null;} }
    private static Integer parameterId(URI uri) {
        for(String param:(uri.getRawQuery()==null?"":uri.getRawQuery()).split("&")) {
            if(param.startsWith("contentId=")) try{return Integer.valueOf(URLDecoder.decode(param.substring(10),StandardCharsets.UTF_8));}catch(Exception e){return null;}
        }
        return null;
    }
}
