\set ON_ERROR_STOP on

-- Snapshot captured from KorService2 searchKeyword2/detailImage2 on 2026-06-24.
-- These are the only itinerary places visible to demo01 after the realistic patch.
BEGIN;

CREATE TEMP TABLE demo_kto_places (
  item_id uuid PRIMARY KEY,
  content_id text NOT NULL,
  title text NOT NULL,
  address text NOT NULL,
  lat numeric(10, 7) NOT NULL,
  lng numeric(10, 7) NOT NULL,
  image_url text NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_kto_places VALUES
  ('d4714833-c902-4ad3-3a50-b765306a89a2', '126508', '경복궁', '서울특별시 종로구 사직로 161 (세종로)', 37.5760307, 126.9767219, 'https://tong.visitkorea.or.kr/cms/resource/98/3487598_image2_1.jpg'),
  ('cfa947f9-d2c0-0e32-6622-23a620ef101c', '126537', '북촌한옥마을', '서울특별시 종로구 계동길 37 (계동)', 37.5790529, 126.9867060, 'https://tong.visitkorea.or.kr/cms/resource/04/3304404_image2_1.jpg'),
  ('0f1a0846-8b80-8a48-8a83-fb7580c62b14', '2650046', '익선동 한옥거리', '서울특별시 종로구 익선동', 37.5734657, 126.9897757, 'https://tong.visitkorea.or.kr/cms/resource/54/3497254_image2_1.jpg'),
  ('b41daeb9-be11-d4cb-6ff0-3a28d72a43b1', '129507', '청계천', '서울특별시 종로구 창신동', 37.5696470, 127.0050743, 'https://tong.visitkorea.or.kr/cms/resource_photo/34/3538134_image2_1.jpg'),
  ('e7cd594c-9fab-7212-3978-40a65e361c1f', '126509', '덕수궁', '서울특별시 중구 세종대로 99 (정동)', 37.5650549, 126.9765746, 'https://tong.visitkorea.or.kr/cms/resource/44/3584644_image2_1.jpg'),
  ('6b651785-82cc-d234-2e7f-4a1e677e3f62', '2470006', '동대문디자인플라자 DDP', '서울특별시 중구 을지로 281 (을지로7가)', 37.5661076, 127.0095710, 'https://tong.visitkorea.or.kr/cms/resource/06/3539606_image2_1.jpg'),
  ('509bfb21-4ee2-c7ba-be7f-792a1db015fd', '126535', '남산서울타워', '서울특별시 용산구 남산공원길 105', 37.5510545, 126.9878821, 'https://tong.visitkorea.or.kr/cms/resource/70/3089670_image2_1.jpg'),
  ('61b92819-5a2f-2136-67d3-6cc2549d01bc', '129703', '국립중앙박물관', '서울특별시 용산구 서빙고로 137 (용산동6가)', 37.5211168, 126.9791124, 'https://tong.visitkorea.or.kr/cms/resource/12/3495012_image2_1.jpg'),
  ('9da3e29b-bd51-597d-bffb-8d065e6c8e6d', '128611', '서울숲', '서울특별시 성동구 뚝섬로 273', 37.5430716, 127.0417984, 'https://tong.visitkorea.or.kr/cms/resource_photo/99/3580599_image2_1.jpg'),
  ('52034fdc-fa2c-a033-894e-a978b975de76', '2650231', '성수동 수제화거리', '서울특별시 성동구 성수동2가 289-30', 37.5444527, 127.0572145, 'https://tong.visitkorea.or.kr/cms/resource/78/2933778_image2_1.bmp'),

  ('e0101010-0000-4000-8000-000000000001', '228853', '용두암', '제주특별자치도 제주시 용두암길 15 (용담이동)', 33.5147813, 126.5125099, 'https://tong.visitkorea.or.kr/cms/resource/68/3011868_image2_1.jpg'),
  ('e0101020-0000-4000-8000-000000000002', '1013246', '동문재래시장', '제주특별자치도 제주시 관덕로14길 20', 33.5115624, 126.5260588, 'https://tong.visitkorea.or.kr/cms/resource/38/2678438_image2_1.jpg'),
  ('e0101030-0000-4000-8000-000000000003', '2660802', '오설록 티뮤지엄', '제주특별자치도 서귀포시 안덕면 신화역사로 15', 33.3060818, 126.2893922, 'https://tong.visitkorea.or.kr/cms/resource/57/3497257_image2_1.jpg'),
  ('e0102010-0000-4000-8000-000000000004', '126435', '성산일출봉', '제주특별자치도 서귀포시 성산읍 일출로 284-12', 33.4581111, 126.9415156, 'https://tong.visitkorea.or.kr/cms/resource/00/2613500_image2_1.jpg'),
  ('e0102020-0000-4000-8000-000000000005', '598696', '소머리오름(우도봉)', '제주특별자치도 제주시 우도면 연평리', 33.4915606, 126.9651406, 'https://tong.visitkorea.or.kr/cms/resource/92/3527092_image2_1.jpg'),
  ('e0103010-0000-4000-8000-000000000006', '126437', '정방폭포', '제주특별자치도 서귀포시 칠십리로214번길 37', 33.2450398, 126.5716053, 'https://tong.visitkorea.or.kr/cms/resource/78/2665278_image2_1.jpg'),
  ('e0103020-0000-4000-8000-000000000007', '127053', '대포주상절리', '제주특별자치도 서귀포시 이어도로 36-24 (중문동)', 33.2389326, 126.4264985, 'https://tong.visitkorea.or.kr/cms/resource/61/3535261_image2_1.jpg'),
  ('e0104010-0000-4000-8000-000000000008', '127635', '한라산', '제주특별자치도 서귀포시 토평동', 33.3618000, 126.5300000, 'https://tong.visitkorea.or.kr/cms/resource_photo/41/3460441_image2_1.jpg'),
  ('e12cd370-9bc8-af79-58a7-8c5dbd27e2f9', '127490', '협재해수욕장', '제주특별자치도 제주시 한림읍 한림로 329-10', 33.3937765, 126.2394419, 'https://tong.visitkorea.or.kr/cms/resource/66/3096066_image2_1.jpg'),
  ('739a712e-2996-7952-36e7-13ddddf90631', '741109', '카멜리아힐', '제주특별자치도 서귀포시 안덕면 병악로 166', 33.2898683, 126.3682547, 'https://tong.visitkorea.or.kr/cms/resource/84/4064284_image2_1.jpg'),

  ('e0201010-0000-4000-8000-000000000011', '126081', '해운대해수욕장', '부산광역시 해운대구 해운대해변로 264', 35.1590840, 129.1602786, 'https://tong.visitkorea.or.kr/cms/resource/34/3090534_image2_1.JPG'),
  ('e0201020-0000-4000-8000-000000000012', '126078', '광안리해수욕장', '부산광역시 수영구 광안해변로 219 (광안동)', 35.1538131, 129.1185478, 'https://tong.visitkorea.or.kr/cms/resource/45/3311245_image2_1.jpg'),
  ('e0201030-0000-4000-8000-000000000013', '2350092', '더베이101', '부산광역시 해운대구 동백로 52', 35.1566217, 129.1520976, 'https://tong.visitkorea.or.kr/cms/resource/41/3407941_image2_1.png'),
  ('e0202010-0000-4000-8000-000000000014', '126848', '해동용궁사', '부산광역시 기장군 기장읍 용궁길 86', 35.1882429, 129.2235302, 'https://tong.visitkorea.or.kr/cms/resource/35/3499335_image2_1.jpg'),
  ('e0202020-0000-4000-8000-000000000015', '1997221', '부산 감천문화마을', '부산광역시 사하구 감내2로 203', 35.0974607, 129.0105970, 'https://tong.visitkorea.or.kr/cms/resource/91/3365491_image2_1.jpg'),
  ('e0203010-0000-4000-8000-000000000016', '126658', '태종대', '부산광역시 영도구 전망로 24 (동삼동)', 35.0596949, 129.0798057, 'https://tong.visitkorea.or.kr/cms/resource/83/3506383_image2_1.jpg'),
  ('e0203020-0000-4000-8000-000000000017', '132190', '부산 자갈치시장', '부산광역시 중구 자갈치해안로 52 (남포동4가)', 35.0966512, 129.0306042, 'https://tong.visitkorea.or.kr/cms/resource/13/2941313_image2_1.bmp'),
  ('9592b84b-5546-1719-bf92-2732c62348e1', '2504464', '부산 송도해상케이블카', '부산광역시 서구 송도해변로 171', 35.0766810, 129.0234248, 'https://tong.visitkorea.or.kr/cms/resource/11/3413711_image2_1.jpg'),
  ('2e140291-68e1-f464-0f27-bf3b2faf7148', '2684712', '흰여울문화마을', '부산광역시 영도구 영선동4가 605-3', 35.0783000, 129.0453000, 'https://tong.visitkorea.or.kr/cms/resource/74/3495874_image2_1.jpg'),

  ('e0401010-0000-4000-8000-000000000031', '126166', '불국사', '경상북도 경주시 불국로 385 (진현동)', 35.7923023, 129.3317254, 'https://tong.visitkorea.or.kr/cms/resource/70/3506170_image2_1.jpg'),
  ('e0401020-0000-4000-8000-000000000032', '126216', '석굴암', '경상북도 경주시 석굴로 238 석굴암', 35.7893407, 129.3507517, 'https://tong.visitkorea.or.kr/cms/resource/69/3581269_image2_1.jpg'),
  ('e0402010-0000-4000-8000-000000000033', '1492402', '경주 대릉원 일원', '경상북도 경주시 황남동 31-1', 35.8382000, 129.2128000, 'https://tong.visitkorea.or.kr/cms/resource/71/4056771_image2_1.jpg'),
  ('e0402020-0000-4000-8000-000000000034', '126207', '첨성대', '경상북도 경주시 첨성로 140-25', 35.8343303, 129.2185345, 'https://tong.visitkorea.or.kr/cms/resource/81/3554381_image2_1.jpg'),
  ('e0403010-0000-4000-8000-000000000035', '126230', '경주 보문관광단지', '경상북도 경주시 보문로 446 (신평동)', 35.8436980, 129.2869680, 'https://tong.visitkorea.or.kr/cms/resource/85/3479385_image2_1.jpg'),
  ('b9d2c1f5-339a-8d84-1e41-ec4f668e3d64', '128526', '경주 동궁과 월지', '경상북도 경주시 원화로 102 (인왕동)', 35.8352024, 129.2283747, 'https://tong.visitkorea.or.kr/cms/resource/62/2612562_image2_1.jpg'),
  ('bf225c07-82cc-64ef-d591-d572548692b4', '2658227', '경주 황리단길', '경상북도 경주시 포석로 1080 (황남동)', 35.8374083, 129.2099537, 'https://tong.visitkorea.or.kr/cms/resource/62/3480062_image2_1.jpg');

UPDATE itinerary.itinerary_items i
SET item_type = 'PLACE',
    place_provider = 'KTO',
    external_place_id = p.content_id,
    place_name = p.title,
    address = p.address,
    lat = p.lat,
    lng = p.lng,
    thumbnail_url = p.image_url,
    source_status = 'AVAILABLE',
    updated_at = now()
FROM demo_kto_places p
WHERE i.id = p.item_id;

DO $$
DECLARE
  expected_count integer;
  updated_count integer;
BEGIN
  SELECT count(*) INTO expected_count FROM demo_kto_places;
  SELECT count(*) INTO updated_count
  FROM itinerary.itinerary_items i
  JOIN demo_kto_places p
    ON p.item_id = i.id
   AND p.content_id = i.external_place_id
   AND i.place_provider = 'KTO';

  IF updated_count <> expected_count THEN
    RAISE EXCEPTION 'Expected to update % KTO demo places, updated %', expected_count, updated_count;
  END IF;
END $$;

COMMIT;
