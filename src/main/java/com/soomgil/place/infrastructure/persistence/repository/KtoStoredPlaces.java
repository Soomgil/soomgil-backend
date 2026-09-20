package com.soomgil.place.infrastructure.persistence.repository;

import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceLiveSearchRequest;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 기존 seed와 API로 수집한 관광지를 같은 DB 조회 경로로 제공한다. */
@Repository
public class KtoStoredPlaces {
    private final NamedParameterJdbcTemplate jdbc;
    public KtoStoredPlaces(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final String SELECT="""
        SELECT a.content_id,a.title,nullif(concat_ws(' ',a.addr1,a.addr2),'') address,a.latitude,a.longitude,
        coalesce((SELECT public_url FROM tourism_source.attraction_images i WHERE i.attraction_no=a.no AND i.is_active ORDER BY display_order,created_at LIMIT 1),nullif(a.first_image1,''),nullif(a.first_image2,'')) thumbnail,
        ct.content_type_name category,a.overview,a.source_modified_at,
        ARRAY(SELECT DISTINCT public_url FROM tourism_source.attraction_images i WHERE i.attraction_no=a.no AND i.is_active) photos
        FROM tourism_source.attractions a LEFT JOIN tourism_source.contenttypes ct ON ct.content_type_id=a.content_type_id WHERE 1=1
        """;
    public Optional<TourismPlaceFeedItem> find(String id) {
        return rows(SELECT+" AND a.content_id::text=:id",Map.of("id",id)).stream().findFirst();
    }
    /** 조회 결과가 적더라도 DB에 있으면 재사용하며 이미 반응한 관광지는 조회 단계에서 제외한다. */
    public List<TourismPlaceFeedItem> search(TourismPlaceLiveSearchRequest request,List<String> excluded,String seed) {
        StringBuilder sql=new StringBuilder(SELECT);
        Map<String,Object> args=new HashMap<>();
        if(request.q()!=null&&!request.q().isBlank()) {sql.append(" AND (a.title ILIKE :q OR coalesce(a.addr1,'') ILIKE :q)");args.put("q","%"+request.q().strip()+"%");}
        if(request.legalRegionCode()!=null&&!request.legalRegionCode().isBlank()) {sql.append(" AND a.area_code::text=:region");args.put("region",request.legalRegionCode());}
        if(request.sigunguCode()!=null&&!request.sigunguCode().isBlank()) {sql.append(" AND a.si_gun_gu_code::text=:district");args.put("district",request.sigunguCode());}
        if(request.category()!=null&&!request.category().isBlank()) {sql.append(" AND (a.content_type_id::text=:category OR ct.content_type_name=:category)");args.put("category",request.category());}
        if(request.bbox()!=null&&!request.bbox().isBlank()) {
            String[] bounds=request.bbox().split(",");
            if(bounds.length!=4) return List.of();
            try { for(int i=0;i<4;i++)args.put("b"+i,Double.parseDouble(bounds[i])); } catch(NumberFormatException error) { return List.of(); }
            sql.append(" AND a.longitude BETWEEN :b0 AND :b2 AND a.latitude BETWEEN :b1 AND :b3");
        }
        if(excluded!=null&&!excluded.isEmpty()) {sql.append(" AND a.content_id::text NOT IN (:excluded)");args.put("excluded",excluded);}
        // 이미지가 있는 장소를 먼저 보여준다(추천 카드가 회색 빈 이미지로 뜨지 않도록). 그 안에서는 seed로 섞는다.
        sql.append(" ORDER BY (nullif(a.first_image1,'') IS NOT NULL OR nullif(a.first_image2,'') IS NOT NULL) DESC, md5(a.content_id::text || :seed) LIMIT :limit");args.put("seed",seed==null?"":seed);args.put("limit",Math.min(100,Math.max(1,request.limit())));
        return rows(sql.toString(),args);
    }
    private List<TourismPlaceFeedItem> rows(String sql,Map<String,?> args) {
        return jdbc.query(sql,args,(rs,row)->new TourismPlaceFeedItem(rs.getString("content_id"),rs.getString("title"),rs.getString("address"),
            rs.getObject("latitude",Double.class),rs.getObject("longitude",Double.class),rs.getString("thumbnail"),rs.getString("category"),rs.getString("overview"),
            Arrays.asList((String[])rs.getArray("photos").getArray()),rs.getObject("source_modified_at",OffsetDateTime.class)));
    }
}
