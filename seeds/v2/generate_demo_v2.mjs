#!/usr/bin/env node
/**
 * Soomgil demo dataset v2 generator.
 *
 * 실제 백엔드 API로 모든 데이터를 만든다(FK·버전·알림이 진짜로 남아 협업한 흔적이 생긴다).
 * 사진은 DB에 이미 있는 KTO 관광지 실제 이미지를 내려받아 미디어 API로 업로드한다.
 *
 * 사전 조건: reset_schema_only.mjs → users_v2.sql 적용 → 백엔드 기동(localhost:8080).
 */
import { spawnSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const API = process.env.SOOMGIL_API || 'http://localhost:8080/api/v1';
const PASSWORD = 'Soomgil123!';
const DB_USER = process.env.DB_USERNAME || 'soomgil';
const DB_NAME = process.env.DB_NAME || 'soomgil';
const PG = spawnSync('docker', ['compose', 'ps', '-q', 'postgres'], { cwd: join(HERE, '..', '..', '..'), encoding: 'utf8' }).stdout.trim();

/* ─────────────────────────── helpers ─────────────────────────── */
const log = (...a) => console.log(new Date().toISOString().slice(11, 19), ...a);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function psql(sql) {
  const r = spawnSync('docker', ['exec', '-i', PG, 'psql', '-U', DB_USER, '-d', DB_NAME, '-X', '-At', '-F', '\t', '-v', 'ON_ERROR_STOP=1'], { input: sql, encoding: 'utf8' });
  if (r.status !== 0) throw new Error('psql failed: ' + r.stderr);
  return r.stdout.trim() ? r.stdout.trim().split('\n').map((l) => l.split('\t')) : [];
}
async function api(token, method, path, body, { raw = false, retries = 2 } = {}) {
  for (let attempt = 0; ; attempt++) {
    const res = await fetch(API + path, {
      method,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (res.status >= 500 && attempt < retries) { await sleep(800 * (attempt + 1)); continue; }
    if (!res.ok) throw new Error(`${method} ${path} → ${res.status} ${await res.text()}`);
    if (raw || res.status === 204) return null;
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  }
}
async function login(email) {
  const r = await api(null, 'POST', '/auth/login', { email, password: PASSWORD });
  return { token: r.accessToken, id: r.user.id, name: r.user.profile.displayName, email };
}
const UA = 'Mozilla/5.0 (Macintosh) SoomgilDemoSeeder/2.0';
async function download(url) {
  const res = await fetch(url.replace(/^http:/, 'https:'), { headers: { 'User-Agent': UA }, redirect: 'follow' });
  if (!res.ok) throw new Error(`download ${url} → ${res.status}`);
  const buf = Buffer.from(await res.arrayBuffer());
  if (buf.length < 8 * 1024 || buf.length > 9_500_000) throw new Error(`bad size ${buf.length} for ${url}`);
  const mime = buf[0] === 0xff && buf[1] === 0xd8 ? 'image/jpeg' : buf[0] === 0x89 && buf[1] === 0x50 ? 'image/png' : null;
  if (!mime) throw new Error(`not jpeg/png: ${url}`);
  return { buf, mime };
}
const manifest = [];
async function uploadImage(user, url, purpose, fileName) {
  const { buf, mime } = await download(url);
  const intent = await api(user.token, 'POST', '/media/upload-urls', { fileName, mimeType: mime, byteSize: buf.length, purpose });
  // 서명된 헤더는 content-length;content-type;host. fetch 가 host/content-length 를 알아서 넣으므로 Content-Type 만 보낸다(호스트 헤더를 넘기면 서명이 깨져 403).
  const put = await fetch(intent.uploadUrl, { method: intent.method || 'PUT', headers: { 'Content-Type': mime }, body: buf });
  if (!put.ok) throw new Error(`presigned upload failed ${put.status}`);
  const media = await api(user.token, 'POST', '/media/files', { objectKey: intent.objectKey, mimeType: mime, byteSize: buf.length });
  manifest.push({ object_key: intent.objectKey, source_url: url, purpose, owner: user.email, media_file_id: media.id, mime, bytes: buf.length });
  return media;
}
const pick = (arr, n, seed = 1) => { const a = [...arr]; let s = seed; const out = []; while (a.length && out.length < n) { s = (s * 9301 + 49297) % 233280; out.push(a.splice(s % a.length, 1)[0]); } return out; };

/* ─────────────────────────── data ─────────────────────────── */
const USERS = [
  [1, 'demo1@soomgil.app'], [2, 'demo2@soomgil.app'], [3, 'seoyeon@soomgil.app'], [4, 'doyun@soomgil.app'], [5, 'hana@soomgil.app'],
  [6, 'jiwoo@soomgil.app'], [7, 'yerin@soomgil.app'], [8, 'taeyang@soomgil.app'], [9, 'sua@soomgil.app'], [10, 'minseok@soomgil.app'],
  [11, 'eunji@soomgil.app'], [12, 'woojin@soomgil.app'], [13, 'chaewon@soomgil.app'], [14, 'hyunwoo@soomgil.app'],
];
const REGIONS = {
  seoul: { label: '서울', codes: ['1111000000'], dest: '서울 종로·중구', areaCode: 1 },
  daejeon: { label: '대전', codes: ['3020000000'], dest: '대전', areaCode: 3 },
  busan: { label: '부산', codes: ['2635000000'], dest: '부산 해운대', areaCode: 6 },
  jeju: { label: '제주', codes: ['5011000000', '5013000000'], dest: '제주', areaCode: 39 },
  gyeongju: { label: '경주', codes: ['4713000000'], dest: '경주', areaCode: 35 },
  gangneung: { label: '강릉', codes: ['4215000000'], dest: '강릉', areaCode: 32 },
};
const TRIPS = [
  { key: 'seoul', owner: 1, members: [2, 3, 4], title: '서울 고궁 산책과 시장 투어', start: '2026-10-09',
    plan: [[126508, 126537, 264353, 132183], [126511, 126510, 2946228, 129507], [126509, 126535]], unscheduled: [970138], vote: true,
    note: '9일 아침 10시 경복궁 정문 집결. 한복 대여는 북촌 쪽이 저렴하대요(2시간 2만원). 광장시장은 육회+빈대떡 조합, 현금 조금 챙기기.\n둘째 날 창덕궁 후원은 예약제(시간당 100명) — 도윤이 예매 담당.',
    dayNote: '경복궁 → 북촌 → 인사동 → 광장시장 순서. 궁 관람 1시간, 북촌은 골목 조용히 걷기(주민 거주지).',
    checklist: ['한복 대여 예약(북촌)', '창덕궁 후원 예매', '광장시장 현금 5만원', '편한 운동화', '보조배터리', '우산(비 예보 확인)', '남산타워 야경 시간 확인'],
    dayChecklist: ['경복궁 입장권(만원 미만 카드 OK)', '북촌 8경 포인트 지도', '인사동 전통차 카페 후보 2개', '광장시장 마약김밥 줄 확인'],
    chat: [[1, '드디어 서울 일정 방 만들었어요 🎉 9일 금요일 10시 경복궁 정문 어때요?'], [2, '좋아요! 저 그날 반차 내놨어요'], [3, '한복 입고 궁 들어가면 무료라던데 우리도 입을까요?'], [4, '오 좋다. 북촌 쪽 대여점이 싸다고 해서 제가 알아볼게요'], [1, '그럼 체크리스트에 한복 대여 넣어둘게요. 도윤은 창덕궁 후원 예매 부탁!'], [4, '넵 예매 완료하면 체크 표시할게요'], [2, '광장시장 육회 진짜 기대됨… 빈대떡도'], [3, '둘째 날 익선동 저녁 어때요? 한옥 골목 예쁘던데'], [1, '익선동 2일차에 넣었어요. 청계천 야경까지 걷는 코스로!'], [2, '마지막 날 남산타워는 해 지는 시간 맞춰 가자'], [1, '투표로 추가 장소 하나 더 뽑아볼까요? 투표 열어둘게요 🗳️'], [3, '스티커 붙였어요!'], [4, '저도요~']] },
  { key: 'daejeon', owner: 2, members: [1, 13, 7], title: '대전 빵지순례 1박 2일', start: '2026-10-24',
    plan: [[1796079, 132505, 1964617, 3061175], [741658, 2782788, 2760707]], unscheduled: [129438, 126846],
    note: '성심당은 본점 오픈(8시) 직후가 줄이 제일 짧아요. 튀김소보로·부추빵은 필수, 딸기시루는 시즌 확인.\n둘째 날 한밭수목원은 유모차 동선 좋아서 예린 가족 합류 OK.',
    dayNote: '성심당 본점 → 으능정이 거리 → 근현대사전시관(옛 충남도청) → 소제동 카페거리 저녁.',
    checklist: ['성심당 빵 보냉백', 'KTX 왕복 예매', '숙소(둔산동) 체크인 15시', '유모차 대여 확인', '아쿠아리움 입장권', '족욕장 수건'],
    dayChecklist: ['성심당 본점 8시 도착', '으능정이 스카이로드 점등 시간', '소제동 저녁 예약'],
    chat: [[2, '대전 방 오픈! 목표는 성심당 세 번 가기 🥐'], [13, '제가 성심당 전문가입니다. 튀소 정석 + 판타롱부추빵 + 보문산메아리 순서로'], [1, '보냉백 챙길게요 ㅋㅋ 빵 사서 서울 가야지'], [7, '아이들 데리고 가는데 둘째 날 수목원 동선 괜찮을까요?'], [2, '한밭수목원 평지라서 유모차 편해요. 아쿠아리움도 근처!'], [7, '완벽! 그럼 저희는 둘째 날 합류할게요'], [13, '첫날 저녁은 소제동 카페거리 어때요? 낡은 관사 개조한 카페들 감성 좋아요'], [1, '찬성. 근현대사전시관 들렀다가 걸어가면 딱이네'], [2, '족욕장은 미정으로 빼둘게요. 시간 남으면 가는 걸로']] },
  { key: 'busan', owner: 1, members: [2, 10, 14], title: '부산 바다 따라 2박 3일', start: '2026-11-06',
    plan: [[126081, 2784330, 2672393], [126080, 126078], [126658, 1018702, 129155]], unscheduled: [2870289, 2763707],
    note: '블루라인파크 스카이캡슐은 주말 매진 빠름 — 미포 → 청사포 편도 예약(현우). 광안리 드론쇼 토요일 19시.\n태종대 다누비열차 마지막 편 17시 20분.',
    dayNote: '해운대 해변 산책 → 미포항 회센터 점심 → 블루라인파크 스카이캡슐(미포→청사포).',
    checklist: ['스카이캡슐 예약(미포 출발)', '광안리 드론쇼 시간 확인', '숙소 해운대 체크인', '자갈치·국제시장 현금', '태종대 다누비열차 시간표', '선크림'],
    dayChecklist: ['해운대 아침 산책 7시', '미포항 회센터 예약', '스카이캡슐 14시'],
    chat: [[1, '부산 간다! 이번엔 바다만 봅시다 🌊'], [14, '미포항 회센터는 제가 아는 집 있어요. 점심 거기로'], [10, '광안리 드론쇼 토요일 저녁이니까 둘째 날 저녁 광안리로 맞추면 됨'], [2, '스카이캡슐 예약 누가 할래요? 저 지난번에 매진돼서 못 탔음'], [14, '제가 미포 출발로 잡을게요. 14시 괜찮죠?'], [1, '네 체크리스트 올렸어요~'], [10, '마지막 날 태종대 다누비 마지막 편 놓치면 걸어 내려와야 해요 ㅋㅋ 시간 체크'], [2, '오륙도는 미정으로 빼두고 시간 되면 가자'], [1, '좋아요. 국제시장에서 씨앗호떡은 필수']] },
  { key: 'jeju', owner: 2, members: [1, 5, 6, 9], title: '제주 동쪽에서 서쪽으로 3박 4일', start: '2026-11-19',
    plan: [[126435, 127813, 598558], [1918639, 126452, 126472], [126438, 2660802, 741109], [572973, 127490, 1013246]], unscheduled: [228854], ai: true,
    note: '렌터카 2대(하나·지우 운전). 첫날 성산일출봉은 일출 시간 06:58 — 5시 40분 숙소 출발.\n우도는 성산항 08:00 배, 도항선에 차는 안 태우고 전기바이크 대여.\n마지막 날 협재 노을 보고 동문시장 야시장에서 마무리.',
    dayNote: '성산일출봉 일출 → 성산항 우도 도항 → 산호해변(서빈백사) → 우도 등대 → 섭지코지 노을.',
    checklist: ['렌터카 2대 예약', '우도 도항선 시간표', '숙소 3박(성산·구좌·애월)', '오설록 티세트 예약', '카멜리아힐 입장권', '바람막이/우비', '멀미약', '동문시장 장바구니'],
    dayChecklist: ['성산일출봉 일출 5:40 출발', '성산항 08:00 배', '우도 전기바이크 대여', '섭지코지 노을 17:30'],
    chat: [[2, '제주 3박 4일 방 열었습니다 🍊 동쪽→서쪽으로 돌아요'], [5, '첫날 일출 보러 성산 가는 거 제가 추천했어요. 5시 40분 출발 가능?'], [6, '가능! 대신 둘째 날 비자림은 천천히 걷게 해줘요'], [9, '숙소는 성산 1박, 구좌 1박, 애월 1박으로 잡을게요. 애월은 한옥스테이!'], [1, '우도는 배에 차 안 태우고 전기바이크 빌리는 게 편해요'], [2, '오케이 체크리스트에 넣었음'], [6, '만장굴 안은 13도라 겉옷 필수'], [5, '셋째 날 오설록 티세트 예약해둘게요'], [9, '카멜리아힐 동백은 12월이 피크인데 11월 말도 초입이라 볼만해요'], [1, '마지막 날 협재에서 노을 보고 동문시장 야시장 가는 코스 어때요'], [2, 'AI한테 동선 한번 요약해달라고 해봤는데 꽤 정리 잘해주네요'], [6, '용머리해안은 미정으로! 물때 봐야 함']] },
  { key: 'gyeongju', owner: 1, members: [2, 11, 3], title: '경주 천년 고도 1박 2일', start: '2026-12-04',
    plan: [[126166, 126216, 2736657], [126207, 1492402, 128526, 2603509, 3451999]], unscheduled: [3022997, 126134, 126228],
    note: '불국사 → 석굴암은 토함산 도로로 15분. 석굴암 본존은 유리 너머 관람.\n둘째 날 동궁과 월지는 해 진 뒤 조명 켜질 때가 진짜 — 17시 이후로 배치.',
    dayNote: '불국사 오전 → 석굴암 → 불국사밀면 점심. 오후는 자유(보문호 산책 or 경주월드).',
    checklist: ['KTX 신경주역 예매', '숙소 황리단길 한옥', '불국사·석굴암 입장권', '동궁과 월지 야경 시간', '문화해설 예약(대릉원 10시)', '따뜻한 옷'],
    dayChecklist: ['불국사 9시 도착', '토함산 도로 안개 확인', '밀면 웨이팅'],
    chat: [[1, '경주 겨울 여행 가요! 은지님 문화재 해설 부탁 🙏'], [11, '기꺼이! 대릉원 해설 10시 예약 잡아둘게요. 천마총 내부도 들어가요'], [3, '황리단길 한옥 숙소 후보 두 개 골랐어요. 방에 사진 올릴게요'], [2, '동궁과 월지는 야경이 진짜라 마지막에 넣어주세요'], [1, '네 둘째 날 17시 이후로 배치했어요'], [11, '석굴암은 사진 촬영 금지니까 눈에 담기'], [3, '불국사 근처 밀면집 있다던데 점심 거기 어때요'], [2, '좋아요. 보문호는 시간 남으면 미정에서 꺼내자'], [1, '경주월드는 겨울엔 운영 시간 짧아서 미정으로 둘게요']] },
  { key: 'gangneung', owner: 2, members: [1, 10, 8], title: '강릉 커피와 바다 1박 2일', start: '2026-12-18',
    plan: [[127722, 3454461, 585522, 264370], [1625166, 129179, 2396259]], unscheduled: [127951, 585526, 2685312],
    note: '안목 커피거리는 오전이 한산. 초당순두부는 짬뽕순두부로 통일?\n둘째 날 정동진 레일바이크 10시 예약(태양). 주문진 곰치국은 겨울 한정.',
    dayNote: '안목해변 커피 → 경포호수광장 산책 → 강문해변 → 초당순두부마을 저녁.',
    checklist: ['KTX 강릉역 예매', '레일바이크 10시 예약', '숙소 경포 오션뷰', '안목 커피 로스터리 리스트', '핫팩', '주문진 시장 현금'],
    dayChecklist: ['안목 커피거리 10시', '경포호 자전거 대여', '초당순두부 저녁 18시'],
    chat: [[2, '올해 마지막 여행은 강릉! 커피 마시고 바다 보고 끝 ☕'], [10, '안목 커피거리 로스터리 리스트 정리해둘게요. 테라로사 본점도 갈까요'], [8, '정동진 레일바이크 10시 예약 제가 할게요. 4인승 하나'], [1, '초당순두부는 짬뽕순두부파 vs 얼큰순두부파 투표 필요'], [2, 'ㅋㅋ 둘 다 시켜'], [10, '경포호는 자전거 빌려서 한 바퀴 돌면 40분이에요'], [8, '주문진 곰치국 겨울 한정이라 둘째 날 아침에 먹자'], [1, '하슬라아트월드는 시간 보고 미정에서 꺼내는 걸로'], [2, '오션뷰 숙소 예약 완료! 체크했어요']] },
];
// 커뮤니티 글: 작성자(사용자 번호), 지역, 사진에 쓸 장소들, 제목/요약/해시태그. 사진은 그 장소들의 KTO 실제 이미지.
const POSTS = [
  { author: 3, region: 'seoul', places: [126508, 126537, 264353], title: '한복 입고 걷는 경복궁과 북촌 반나절', summary: '경복궁은 한복을 입으면 무료입장이라 북촌 대여점에서 빌려 입고 들어갔어요. 근정전 앞 광장에서 사진 찍고, 북촌 골목은 주민이 사는 곳이라 조용히 걸었습니다. 인사동 전통차 카페에서 쌍화차로 마무리.', tags: ['서울', '경복궁', '북촌한옥마을', '한복', '고궁산책'] },
  { author: 4, region: 'seoul', places: [132183, 129507, 126510], title: '광장시장 먹방 루트 — 육회, 빈대떡, 마약김밥', summary: '토요일 점심 광장시장. 육회골목은 줄이 길어도 회전이 빨라요. 빈대떡은 갓 부친 걸 서서 먹는 게 정석. 배 채우고 종묘 돌담길 따라 청계천까지 소화 산책했습니다.', tags: ['서울', '광장시장', '먹방', '청계천', '주말나들이'] },
  { author: 13, region: 'daejeon', places: [1796079, 132505, 3061175], title: '성심당 본점 오픈런 후기와 소제동 카페 산책', summary: '8시 오픈 직후 도착하면 줄이 거의 없어요. 튀김소보로·판타롱부추빵·보문산메아리까지 야무지게 담고 으능정이 거리를 지나 소제동으로. 옛 철도 관사를 개조한 카페들이 골목마다 숨어 있어요.', tags: ['대전', '성심당', '소제동', '빵지순례', '카페거리'] },
  { author: 7, region: 'daejeon', places: [741658, 2782788, 2760707], title: '아이와 함께한 한밭수목원·아쿠아리움 하루', summary: '유모차로 다니기 좋은 대전 코스. 한밭수목원 동원·서원 모두 평지라 편했고, 엑스포 아쿠아리움은 실내라 날씨 상관없이 좋아요. 마무리는 유성온천 족욕장에서 다리 풀기.', tags: ['대전', '한밭수목원', '가족여행', '유모차', '아쿠아리움'] },
  { author: 10, region: 'busan', places: [126081, 2784330, 2672393], title: '미포에서 청사포까지, 스카이캡슐 타고 본 해운대', summary: '해운대 아침 산책으로 시작해 미포항 회센터에서 점심. 블루라인파크 스카이캡슐은 미포 출발이 바다 방향이라 좋아요. 청사포에서 내려 다릿돌전망대까지 걸었습니다.', tags: ['부산', '해운대', '블루라인파크', '스카이캡슐', '바다여행'] },
  { author: 14, region: 'busan', places: [126658, 129155, 2870289], title: '태종대 다누비열차와 오륙도 해맞이', summary: '태종대는 다누비열차로 한 바퀴 돌고 전망대에서 내려 걷는 게 정석. 다음 날 새벽 오륙도 해맞이공원에서 일출을 봤는데 스카이워크 유리 바닥이 생각보다 아찔합니다.', tags: ['부산', '태종대', '오륙도', '일출', '영도'] },
  { author: 5, region: 'jeju', places: [126435, 598558, 129147], title: '성산일출봉 일출 후 우도 한 바퀴', summary: '일출 30분 전에 정상 도착하면 자리가 넉넉해요. 내려와서 성산항 8시 배로 우도 입도. 산호해변(서빈백사)의 하얀 홍조단괴 해변이 진짜 예쁘고, 우도 등대까지 전기바이크로 돌았습니다.', tags: ['제주', '성산일출봉', '우도', '일출', '전기바이크'] },
  { author: 6, region: 'jeju', places: [126472, 126452, 1918639], title: '비자림 숲길과 만장굴, 월정리 노을', summary: '비자림은 송이길이라 발이 편하고 2코스가 조용해요. 만장굴 내부는 13도라 겉옷 필수. 해 질 무렵 월정리 해변 카페 2층에서 노을을 봤습니다.', tags: ['제주', '비자림', '만장굴', '월정리', '숲길'] },
  { author: 9, region: 'jeju', places: [2660802, 741109, 572973], title: '서귀포 서쪽 — 오설록, 카멜리아힐, 새별오름', summary: '오설록 티뮤지엄에서 녹차 아이스크림, 카멜리아힐은 11월 말 동백이 피기 시작. 마지막은 새별오름 억새. 해 질 때 능선 실루엣이 아름다워요.', tags: ['제주', '오설록', '카멜리아힐', '새별오름', '억새'] },
  { author: 11, region: 'gyeongju', places: [126166, 126207, 128526], title: '불국사에서 동궁과 월지까지, 경주 하루 코스', summary: '불국사는 오전 빛이 좋고 석굴암은 안개 낀 토함산 도로가 운치 있어요. 오후에 첨성대·대릉원을 해설과 함께 돌고, 동궁과 월지는 조명이 켜진 뒤 연못 반영을 봐야 합니다.', tags: ['경주', '불국사', '동궁과월지', '첨성대', '역사여행'] },
  { author: 12, region: 'gyeongju', places: [2603509, 3451999, 126134], title: '혼자 걷는 황리단길과 월정교 야경', summary: '혼행으로 다녀온 경주. 황리단길은 평일 오후가 한산하고, 월정교는 해 진 직후 조명 켜질 때가 제일 예쁩니다. 보문호 산책로는 아침에 안개가 끼면 그림 같아요.', tags: ['경주', '황리단길', '월정교', '혼행', '야경'] },
  { author: 8, region: 'gangneung', places: [2396259, 1625166, 129179], title: '정동진 레일바이크와 주문진 항구 풍경', summary: '정동진역에서 출발하는 레일바이크는 바다를 옆에 두고 달려요. 오후엔 주문진항에서 회 한 접시, 등대까지 걸어 올라가면 항구가 한눈에 들어옵니다.', tags: ['강릉', '정동진', '레일바이크', '주문진', '항구'] },
  { author: 10, region: 'gangneung', places: [127722, 3454461, 264370], title: '안목 커피거리에서 경포호, 초당순두부까지', summary: '강릉 커피 여행의 정석 코스. 안목해변 로스터리에서 핸드드립 한 잔, 경포호 자전거 한 바퀴(40분), 저녁은 초당순두부마을에서 짬뽕순두부. 겨울 바다는 역시 강릉.', tags: ['강릉', '안목해변', '커피', '경포호', '초당순두부'] },
  { author: 3, region: 'gangneung', places: [127951, 585522, 585526], title: '하슬라아트월드와 강문해변 산책', summary: '바다 위 미술관 하슬라아트월드는 야외 조각공원이 압도적이에요. 강문해변 솟대다리에서 사진 찍고 사천진해변까지 드라이브했습니다.', tags: ['강릉', '하슬라아트월드', '강문해변', '미술관', '드라이브'] },
  { author: 1, region: 'seoul', places: [126511, 2946228, 126509], title: '창경궁 대온실과 익선동 저녁 산책', summary: '창경궁 대온실은 근대 유리온실인데 겨울에도 초록이 가득해요. 해 지고 익선동 한옥골목으로 넘어가 저녁, 덕수궁 돌담길로 마무리. 숨길로 함께 짠 첫 일정이라 더 기억에 남네요.', tags: ['서울', '창경궁', '익선동', '덕수궁', '야간산책'] },
  { author: 1, region: 'busan', places: [126080, 126078, 1018702], title: '송정에서 광안리까지 — 부산 해변 하루', summary: '송정은 서핑하는 사람들 구경만 해도 재밌고, 광안리는 토요일 저녁 드론쇼가 하이라이트. 국제시장 먹자골목에서 씨앗호떡으로 마무리했습니다.', tags: ['부산', '송정해수욕장', '광안리', '드론쇼', '국제시장'] },
  { author: 2, region: 'jeju', places: [126438, 127490, 1013246], title: '천지연폭포·협재 노을·동문시장 야시장', summary: '천지연폭포는 밤에도 조명이 켜져 산책 좋고, 협재 해변 노을은 비양도 실루엣이 포인트. 동문시장 야시장 딱새우회와 흑돼지 꼬치로 마무리하면 완벽.', tags: ['제주', '천지연폭포', '협재해수욕장', '동문시장', '야시장'] },
  { author: 2, region: 'daejeon', places: [1964617, 129438, 126846], title: '옛 충남도청과 장태산 메타세쿼이아, 대청호 드라이브', summary: '대전근현대사전시관(옛 충남도청)은 근대 건축 그 자체. 장태산자연휴양림 스카이워크에서 메타세쿼이아 숲을 위에서 내려다보고, 대청호 오백리길 드라이브로 하루를 닫았어요.', tags: ['대전', '장태산', '대청호', '근대건축', '드라이브'] },
];
const COMMENTS = ['사진 너무 좋아요 저도 이 코스 그대로 따라가 볼게요!', '여기 주차는 어땠어요?', '아침 일찍 가는 게 진리네요 👍', '다음 달 갈 예정인데 딱 필요한 정보였어요', '저장했습니다! 리트립으로 가져가서 일정 짜볼게요', '날씨 좋을 때 가셨네요 부럽다', '이 근처 맛집도 추천 부탁드려요', '아이 데리고 가도 괜찮을까요?', '걷는 거리가 얼마나 되나요?', '노을 시간대 팁 감사합니다', '와 이 사진 그림 같아요', '동선 짜기 딱 좋게 정리해주셨네요'];
const REPLIES = ['주차는 공영주차장이 넉넉했어요! 주말은 일찍 가세요', '왕복 5km 정도라 운동화 추천이에요', '아이랑도 충분히 가능해요, 유모차도 OK', '감사합니다 ☺️ 좋은 여행 되세요!', '근처 맛집은 다음 글에 정리해볼게요'];

/* ─────────────────────────── main ─────────────────────────── */
async function main() {
  if (!PG) throw new Error('postgres container not found');
  log('login users');
  const users = {};
  for (const [n, email] of USERS) { users[n] = await login(email); }
  const demo1 = users[1], demo2 = users[2];

  // 장소 마스터(좌표/주소/사진) — content_id 로 DB에서 읽는다.
  const allIds = new Set([...TRIPS.flatMap((t) => [...t.plan.flat(), ...t.unscheduled]), ...POSTS.flatMap((p) => p.places)]);
  const rows = psql(`SELECT a.content_id, a.title, coalesce(a.addr1,''), a.latitude, a.longitude, coalesce(a.first_image1,''), coalesce(a.first_image2,''),
      coalesce((SELECT string_agg(ai.public_url, '|' ORDER BY ai.display_order) FROM tourism_source.attraction_images ai WHERE ai.attraction_no=a.no AND ai.is_active AND ai.public_url ~ '^https?://'), '')
    FROM tourism_source.attractions a WHERE a.content_id IN (${[...allIds].join(',')});`);
  const PLACE = {};
  // psql -A 는 마지막 컬럼이 비어 있으면 뒤 탭을 생략하므로 imgs 는 기본값을 둔다.
  for (const [id, title, addr, lat, lng, img1 = '', img2 = '', imgs = ''] of rows) {
    PLACE[id] = { id: String(id), title: title.replace(/\s*\[.*?\]\s*/g, '').replace(/\s*\(.*?국가지질공원\)\s*/g, '').trim(), addr, lat: Number(lat), lng: Number(lng),
      images: [...new Set([img1, img2, ...imgs.split('|')].filter((u) => /^https?:\/\//.test(u)))] };
  }
  for (const id of allIds) if (!PLACE[id]) throw new Error(`place ${id} missing in tourism_source.attractions`);
  log(`places loaded: ${Object.keys(PLACE).length}`);

  // 1) 프로필 사진 (KTO 풍경 사진을 프로필로) + 데모 계정 온보딩(가입 취향 설문 10곳 → 실제 스와이프)
  log('profile images');
  const avatarPool = [126435, 126472, 127722, 126081, 126508, 2603509, 1918639, 585522, 126438, 129507, 126207, 741109, 3454461, 2784330];
  for (const [n] of USERS) {
    const u = users[n];
    // 후보 사진을 순서대로 시도한다(bmp 등 비지원 포맷이면 다음 후보로).
    const candidates = [0, 1, 2, 3].flatMap((k) => PLACE[avatarPool[(n - 1 + k) % avatarPool.length]].images);
    let done = false;
    for (const src of candidates) {
      try { const m = await uploadImage(u, src, 'PROFILE_IMAGE', `avatar-${n}.jpg`); await api(u.token, 'PATCH', '/me', { profileMediaFileId: m.id }); done = true; break; }
      catch (e) { log(`  avatar retry for ${u.email}: ${e.message.slice(0, 90)}`); }
    }
    if (!done) log(`  avatar FAILED for ${u.email}`);
  }
  log('onboarding survey for demo accounts');
  for (const u of [demo1, demo2]) {
    const survey = await api(u.token, 'GET', '/onboarding/preference-survey');
    if (!survey.completed) {
      const reactions = ['LIKE', 'SUPER_LIKE', 'LIKE', 'NOPE', 'LIKE', 'LIKE', 'SUPER_LIKE', 'NOPE', 'LIKE', 'LIKE'];
      await api(u.token, 'PUT', '/onboarding/preference-survey/responses', { surveyVersionId: survey.surveyVersionId, responses: survey.places.map((p, i) => ({ provider: p.provider, externalPlaceId: p.externalPlaceId, reaction: reactions[i % reactions.length] })) });
    }
  }

  // 2) 팔로우
  log('follows');
  for (const [n] of USERS) if (n > 2) { await api(demo1.token, 'PUT', `/users/${users[n].id}/follow`).catch(() => {}); await api(users[n].token, 'PUT', `/users/${demo1.id}/follow`).catch(() => {}); }
  for (const n of [3, 5, 6, 9, 10, 12, 13, 14]) { await api(demo2.token, 'PUT', `/users/${users[n].id}/follow`).catch(() => {}); await api(users[n].token, 'PUT', `/users/${demo2.id}/follow`).catch(() => {}); }
  await api(demo1.token, 'PUT', `/users/${demo2.id}/follow`).catch(() => {}); await api(demo2.token, 'PUT', `/users/${demo1.id}/follow`).catch(() => {});

  // 3) 여행방 6개 (일정·경로·메모·체크리스트·채팅·멤버)
  const tripIds = {};
  for (const spec of TRIPS) {
    const owner = users[spec.owner]; const region = REGIONS[spec.key];
    log(`trip: ${spec.title}`);
    const trip = await api(owner.token, 'POST', '/trips', { title: spec.title, displayDestination: region.dest, legalRegionCodes: region.codes });
    tripIds[spec.key] = trip.id; let version = trip.itineraryVersion ?? 0;
    // 멤버 초대 → 수락 (초대 1장 = 1명)
    for (const m of spec.members) { const inv = await api(owner.token, 'POST', `/trips/${trip.id}/invites`, {}); await api(users[m].token, 'POST', `/trip-invites/${encodeURIComponent(inv.inviteCode)}/accept`, {}); }
    // 일차 + 장소
    const dayIds = []; const itemIdsByDay = [];
    for (let d = 0; d < spec.plan.length; d++) {
      const date = new Date(spec.start + 'T00:00:00Z'); date.setUTCDate(date.getUTCDate() + d);
      const r = await api(owner.token, 'POST', `/trips/${trip.id}/itinerary/days`, { baseVersion: version, groupType: 'DAY', dayNumber: d + 1, date: date.toISOString().slice(0, 10), sortOrder: d });
      version = r.itineraryVersion; dayIds.push(r.day.id); itemIdsByDay.push([]);
      const actors = [owner, ...spec.members.map((m) => users[m])];
      for (let i = 0; i < spec.plan[d].length; i++) {
        const p = PLACE[spec.plan[d][i]]; const actor = actors[(d + i) % actors.length]; // 여러 멤버가 번갈아 장소를 추가한다
        const ri = await api(actor.token, 'POST', `/trips/${trip.id}/itinerary/items`, { baseVersion: version, itineraryDayId: r.day.id, sortOrder: i, itemType: 'PLACE', place: { provider: 'KTO', externalPlaceId: p.id }, placeName: p.title, address: p.addr, lat: p.lat, lng: p.lng, thumbnailUrl: p.images[0] || null });
        version = ri.itineraryVersion; itemIdsByDay[d].push({ id: ri.item.id, p });
      }
    }
    const un = await api(owner.token, 'POST', `/trips/${trip.id}/itinerary/days`, { baseVersion: version, groupType: 'UNSCHEDULED', sortOrder: spec.plan.length });
    version = un.itineraryVersion;
    for (let i = 0; i < spec.unscheduled.length; i++) { const p = PLACE[spec.unscheduled[i]]; const ri = await api(owner.token, 'POST', `/trips/${trip.id}/itinerary/items`, { baseVersion: version, itineraryDayId: un.day.id, sortOrder: i, itemType: 'PLACE', place: { provider: 'KTO', externalPlaceId: p.id }, placeName: p.title, address: p.addr, lat: p.lat, lng: p.lng, thumbnailUrl: p.images[0] || null }); version = ri.itineraryVersion; }
    // 경로(연속 장소 사이, Mapbox 맵매칭). 실패해도 계속.
    let routes = 0;
    for (const items of itemIdsByDay) for (let i = 0; i + 1 < items.length; i++) {
      const a = items[i], b = items[i + 1]; const dist = Math.hypot(a.p.lat - b.p.lat, (a.p.lng - b.p.lng) * 0.8) * 111;
      try { const rr = await api(owner.token, 'POST', `/trips/${trip.id}/itinerary/routes/map-match`, { baseVersion: version, originItineraryItemId: a.id, destinationItineraryItemId: b.id, mode: dist < 2.5 ? 'WALKING' : 'DRIVING', coordinates: [{ lng: a.p.lng, lat: a.p.lat }, { lng: b.p.lng, lat: b.p.lat }] }); version = rr.itineraryVersion; routes++; }
      catch (e) { log(`  route skipped (${a.p.title}→${b.p.title}): ${e.message.slice(0, 90)}`); }
    }
    // 메모(여행/1일차) + 체크리스트(여행 준비물 + 1일차) + 멤버별 완료 상태
    await api(owner.token, 'PUT', `/trips/${trip.id}/planning/notes`, { scopeType: 'TRIP', itineraryDayId: null, content: spec.note, baseVersion: 0 });
    await api(users[spec.members[0]].token, 'PUT', `/trips/${trip.id}/planning/notes`, { scopeType: 'DAY', itineraryDayId: dayIds[0], content: spec.dayNote, baseVersion: 0 });
    await api(owner.token, 'PUT', `/trips/${trip.id}/planning/checklists`, { scopeType: 'TRIP', itineraryDayId: null, title: '여행 준비물' });
    await api(users[spec.members[1]].token, 'PUT', `/trips/${trip.id}/planning/checklists`, { scopeType: 'DAY', itineraryDayId: dayIds[0], title: '1일차 체크' });
    const lists = await api(owner.token, 'GET', `/trips/${trip.id}/planning/checklists`);
    const tripList = lists.find((c) => c.scopeType === 'TRIP'); const dayList = lists.find((c) => c.scopeType === 'DAY');
    const members = [owner, ...spec.members.map((m) => users[m])];
    for (let i = 0; i < spec.checklist.length; i++) {
      const r = await api(members[i % members.length].token, 'POST', `/trips/${trip.id}/planning/checklists/${tripList.id}/items`, { content: spec.checklist[i], sortOrder: i });
      const item = (r.checklist?.items || []).find((x) => x.content === spec.checklist[i]) || (await api(owner.token, 'GET', `/trips/${trip.id}/planning/checklists`)).find((c) => c.id === tripList.id).items.find((x) => x.content === spec.checklist[i]);
      // 완료 상태: 항목마다 다른 멤버 조합이 체크한다(협업 느낌)
      for (let k = 0; k < members.length; k++) if ((i + k) % 3 !== 0) await api(members[k].token, 'PATCH', `/trips/${trip.id}/planning/checklists/${tripList.id}/items/${item.id}/members/me`, { isCompleted: true }).catch(() => {});
    }
    for (let i = 0; i < spec.dayChecklist.length; i++) {
      await api(members[(i + 1) % members.length].token, 'POST', `/trips/${trip.id}/planning/checklists/${dayList.id}/items`, { content: spec.dayChecklist[i], sortOrder: i });
    }
    const dayItems = (await api(owner.token, 'GET', `/trips/${trip.id}/planning/checklists`)).find((c) => c.id === dayList.id).items;
    for (let i = 0; i < dayItems.length; i++) if (i % 2 === 0) await api(members[i % members.length].token, 'PATCH', `/trips/${trip.id}/planning/checklists/${dayList.id}/items/${dayItems[i].id}/members/me`, { isCompleted: true }).catch(() => {});
    // 채팅
    for (const [who, text] of spec.chat) await api(users[who].token, 'POST', `/trips/${trip.id}/chat/messages`, { content: text });
    log(`  members=${members.length} days=${dayIds.length} items=${itemIdsByDay.flat().length + spec.unscheduled.length} routes=${routes} chat=${spec.chat.length}`);
  }

  // 4) 스와이프(지역 피드) + 저장 장소 — 데모 계정 20+ (온보딩 10 포함 30), 협업 멤버는 각자 지역 10개
  log('swipes');
  // category = KTO content_type_id (12 관광지, 14 문화시설, 28 레포츠, 39 음식점). 관광지 위주로 취향을 쌓아 추천·투표 후보가 볼 만해지게 한다.
  async function swipeRegion(u, code, n, seed, category = '12') {
    const feed = await api(u.token, 'GET', `/swipe/feed?legalRegionCode=${code}&limit=${n}&excludeRecent=true&category=${category}`);
    const rx = ['LIKE', 'LIKE', 'SUPER_LIKE', 'NOPE', 'LIKE', 'NOPE', 'LIKE', 'SUPER_LIKE'];
    let i = 0; for (const it of feed.items || []) { await api(u.token, 'PUT', `/places/${it.place.provider}/${it.place.externalPlaceId}/swipe-reaction`, { reaction: rx[(i + seed) % rx.length] }).catch(() => {}); i++; }
    return i;
  }
  let s1 = 0, s2 = 0;
  s1 += await swipeRegion(demo1, '1111000000', 8, 0, '12'); s1 += await swipeRegion(demo1, '1111000000', 4, 1, '14'); s1 += await swipeRegion(demo1, '2635000000', 8, 2, '12');
  s2 += await swipeRegion(demo2, '5011000000', 8, 1, '12'); s2 += await swipeRegion(demo2, '5011000000', 4, 2, '28'); s2 += await swipeRegion(demo2, '4215000000', 8, 3, '12');
  for (const spec of TRIPS) for (const m of spec.members) if (m > 2) { await swipeRegion(users[m], REGIONS[spec.key].codes[0], 6, m, '12'); await swipeRegion(users[m], REGIONS[spec.key].codes[0], 3, m + 1, '39'); }
  for (const id of [126508, 126081, 126207, 127722]) await api(demo1.token, 'PUT', `/places/KTO/${id}/save`).catch(() => {});
  for (const id of [126435, 1918639, 3454461, 1796079]) await api(demo2.token, 'PUT', `/places/KTO/${id}/save`).catch(() => {});
  log(`  demo1 feed swipes=${s1} (+10 onboarding), demo2=${s2} (+10 onboarding)`);

  // 5) 투표 (서울 방): 방장이 열고 멤버 전원이 스티커 제출 → 종료 → 선정 장소가 일정에 반영
  log('vote on seoul trip');
  try {
    const t = tripIds.seoul; const owner = demo1; const voters = [demo1, demo2, users[3], users[4]];
    const session = await api(owner.token, 'POST', `/trips/${t}/vote-sessions`, { stickerAllowance: 3, selectionCount: 3, candidateCount: 10 });
    for (let v = 0; v < voters.length; v++) {
      const st = await api(voters[v].token, 'GET', `/trips/${t}/vote-sessions/current`);
      const cands = st.session.candidates; const a = cands[v % cands.length], b = cands[(v + 2) % cands.length];
      const placements = a.id === b.id ? [{ candidateId: a.id, stickerCount: 3 }] : [{ candidateId: a.id, stickerCount: 2 }, { candidateId: b.id, stickerCount: 1 }];
      await api(voters[v].token, 'PUT', `/trips/${t}/vote-sessions/${session.id}/my-stickers`, { placements });
      await api(voters[v].token, 'POST', `/trips/${t}/vote-sessions/${session.id}/my-submission`, {});
    }
    const result = await api(owner.token, 'POST', `/trips/${t}/vote-sessions/${session.id}/completion`, { acknowledgeUnvotedParticipants: true });
    log(`  vote completed: selected=${result.results.filter((r) => r.selected).map((r) => r.name).join(', ')}`);
  } catch (e) { log(`  vote skipped: ${e.message.slice(0, 160)}`); }

  // 6) 커뮤니티: 작성자별 여행방(1일) + 실제 KTO 사진 업로드 + 게시 + 좋아요/댓글/리트립
  log('community posts');
  const postIds = [];
  for (let pi = 0; pi < POSTS.length; pi++) {
    const spec = POSTS[pi]; const author = users[spec.author]; const region = REGIONS[spec.region];
    let sourceTrip = spec.author <= 2 ? { id: tripIds[spec.region] } : null;
    if (!sourceTrip) {
      const t = await api(author.token, 'POST', '/trips', { title: `${region.label} ${spec.title.split(/[,—·]/)[0].trim().slice(0, 40)}`, displayDestination: region.dest, legalRegionCodes: region.codes });
      let v = t.itineraryVersion ?? 0;
      const d = await api(author.token, 'POST', `/trips/${t.id}/itinerary/days`, { baseVersion: v, groupType: 'DAY', dayNumber: 1, date: `2026-0${6 + (pi % 3)}-${String(5 + pi).padStart(2, '0')}`, sortOrder: 0 }); v = d.itineraryVersion;
      for (let i = 0; i < spec.places.length; i++) { const p = PLACE[spec.places[i]]; const ri = await api(author.token, 'POST', `/trips/${t.id}/itinerary/items`, { baseVersion: v, itineraryDayId: d.day.id, sortOrder: i, itemType: 'PLACE', place: { provider: 'KTO', externalPlaceId: p.id }, placeName: p.title, address: p.addr, lat: p.lat, lng: p.lng, thumbnailUrl: p.images[0] || null }); v = ri.itineraryVersion; }
      sourceTrip = { id: t.id };
    }
    const detail = await api(author.token, 'GET', `/trips/${sourceTrip.id}`);
    // 사진: 글에 등장하는 장소들의 실제 KTO 이미지 (최대 4장)
    const urls = [...new Set(spec.places.flatMap((id) => PLACE[id].images))].slice(0, 4);
    const media = [];
    for (let i = 0; i < urls.length; i++) { try { media.push(await uploadImage(author, urls[i], 'COMMUNITY_POST', `post-${pi + 1}-${i + 1}.jpg`)); } catch (e) { log(`  photo skipped: ${e.message.slice(0, 100)}`); } }
    if (!media.length) throw new Error(`no photos for post "${spec.title}"`);
    const post = await api(author.token, 'POST', '/stories', { sourceTripId: sourceTrip.id, baseVersion: detail.itineraryVersion, visibility: 'PUBLIC', title: spec.title, summary: spec.summary, coverMediaFileId: media[0].id, mediaFileIds: media.map((m) => m.id), hashtags: spec.tags });
    postIds.push({ id: post.id, author: spec.author, region: spec.region });
    // 좋아요: 작성자 제외 3~11명
    const likers = pick(USERS.map(([n]) => n).filter((n) => n !== spec.author), 3 + (pi * 7) % 9, pi + 1);
    for (const n of likers) await api(users[n].token, 'POST', `/stories/${post.id}/likes`).catch(() => {});
    // 댓글 2~5 + 답글
    const commenters = pick(USERS.map(([n]) => n).filter((n) => n !== spec.author), 2 + (pi % 4), pi + 11);
    for (let c = 0; c < commenters.length; c++) {
      const cm = await api(users[commenters[c]].token, 'POST', `/stories/${post.id}/comments`, { content: COMMENTS[(pi + c) % COMMENTS.length], parentCommentId: null });
      if (c === 0) await api(author.token, 'POST', `/stories/${post.id}/comments`, { content: REPLIES[pi % REPLIES.length], parentCommentId: cm.id }).catch(() => {});
    }
    log(`  post ${pi + 1}/${POSTS.length} "${spec.title}" photos=${media.length} likes=${likers.length} comments=${commenters.length}`);
  }
  // 리트립: 데모 계정이 다른 사람 글을 내 여행으로 가져온다
  const jejuPost = postIds.find((p) => p.region === 'jeju' && p.author !== 2); const gnPost = postIds.find((p) => p.region === 'gangneung' && p.author !== 1);
  if (jejuPost) await api(demo2.token, 'POST', `/stories/${jejuPost.id}/retrip`, { title: '제주 우도 코스 따라가기' }).catch((e) => log('  retrip skipped: ' + e.message.slice(0, 100)));
  if (gnPost) await api(demo1.token, 'POST', `/stories/${gnPost.id}/retrip`, { title: '강릉 레일바이크 코스 저장' }).catch((e) => log('  retrip skipped: ' + e.message.slice(0, 100)));

  // 7) AI 가이드 대화 (실제 모델 호출) — 데모 방 2곳
  log('ai chat');
  for (const [u, key, q] of [[demo1, 'seoul', '멤버 취향에 맞는 장소 두 곳 추천해줘'], [demo2, 'jeju', '현재 일정을 3줄로 요약해줘']]) {
    try { const code = REGIONS[key].codes[0]; const vp = await fetch(`${API}/places/region-viewport?legalRegionCode=${code}`, { headers: { Authorization: `Bearer ${u.token}` } }).then((r) => (r.status === 200 ? r.json() : null));
      await api(u.token, 'POST', `/trips/${tripIds[key]}/ai/messages`, { content: q, baseVersion: null, viewport: vp ? { minLng: vp.minLng, minLat: vp.minLat, maxLng: vp.maxLng, maxLat: vp.maxLat } : null }); }
    catch (e) { log(`  ai skipped: ${e.message.slice(0, 120)}`); }
  }

  // 8) 시간 흐름 만들기: 방·글·댓글·채팅이 최근 몇 주에 걸쳐 생긴 것처럼 created_at 을 뒤로 흩뿌린다.
  log('backdating timestamps');
  psql(`
    UPDATE trip.trips t SET created_at = now() - (interval '1 day' * (7 + (('x'||substr(md5(t.id::text),1,6))::bit(24)::int % 50))), updated_at = now() - (interval '1 hour' * (('x'||substr(md5(t.id::text),1,4))::bit(16)::int % 72));
    UPDATE trip.trip_members m SET joined_at = t.created_at + (interval '1 hour' * (('x'||substr(md5(m.id::text),1,4))::bit(16)::int % 48)) FROM trip.trips t WHERE t.id = m.trip_id AND m.role <> 'OWNER';
    UPDATE trip.trip_members m SET joined_at = t.created_at FROM trip.trips t WHERE t.id = m.trip_id AND m.role = 'OWNER';
    WITH ordered AS (SELECT c.id, c.trip_id, row_number() OVER (PARTITION BY c.trip_id ORDER BY c.created_at) rn, count(*) OVER (PARTITION BY c.trip_id) cnt FROM chat.trip_chat_messages c)
    UPDATE chat.trip_chat_messages c SET created_at = t.created_at + interval '2 days' + (interval '1 minute' * (o.rn * 37 + (o.rn % 3) * 400)) FROM ordered o JOIN trip.trips t ON t.id = o.trip_id WHERE c.id = o.id;
    UPDATE community.posts p SET published_at = now() - (interval '1 day' * (1 + (('x'||substr(md5(p.id::text),1,6))::bit(24)::int % 44))) - (interval '1 minute' * (('x'||substr(md5(p.id::text),1,4))::bit(16)::int % 700)), created_at = now() - (interval '1 day' * (1 + (('x'||substr(md5(p.id::text),1,6))::bit(24)::int % 44))), updated_at = now() - interval '1 hour';
    UPDATE community.post_comments c SET created_at = p.published_at + (interval '1 hour' * (2 + (('x'||substr(md5(c.id::text),1,4))::bit(16)::int % 90))), updated_at = p.published_at + interval '4 hours' FROM community.posts p WHERE p.id = c.post_id;
    UPDATE community.post_likes l SET created_at = p.published_at + (interval '1 minute' * (30 + (('x'||substr(md5(l.post_id::text||l.user_id::text),1,4))::bit(16)::int % 4000))) FROM community.posts p WHERE p.id = l.post_id;
    UPDATE planning.trip_notes n SET created_at = t.created_at + interval '1 day', updated_at = t.created_at + interval '3 days' FROM trip.trips t WHERE t.id = n.trip_id;
    UPDATE planning.checklists c SET created_at = t.created_at + interval '1 day', updated_at = t.created_at + interval '2 days' FROM trip.trips t WHERE t.id = c.trip_id;
    UPDATE preference.user_saved_places SET created_at = now() - (interval '1 day' * (2 + (('x'||substr(md5(id::text),1,4))::bit(16)::int % 30)));
    UPDATE notification.notifications n SET created_at = now() - (interval '1 hour' * (1 + (('x'||substr(md5(n.id::text),1,4))::bit(16)::int % 400)));
  `);

  // 9) 미디어 매니페스트 (AWS S3 재업로드용: object_key ↔ KTO 원본 URL)
  const csv = ['object_key,source_url,purpose,owner,media_file_id,mime,bytes', ...manifest.map((m) => [m.object_key, m.source_url, m.purpose, m.owner, m.media_file_id, m.mime, m.bytes].join(','))].join('\n');
  writeFileSync(join(HERE, 'media-manifest-v2.csv'), csv + '\n');
  const summary = psql(`SELECT 'users='||(SELECT count(*) FROM auth.users)||' trips='||(SELECT count(*) FROM trip.trips)||' items='||(SELECT count(*) FROM itinerary.itinerary_items)||' routes='||(SELECT count(*) FROM itinerary.trip_routes)||' notes='||(SELECT count(*) FROM planning.trip_notes)||' checklist_items='||(SELECT count(*) FROM planning.checklist_items)||' chat='||(SELECT count(*) FROM chat.trip_chat_messages)||' posts='||(SELECT count(*) FROM community.posts)||' post_media='||(SELECT count(*) FROM community.post_media)||' comments='||(SELECT count(*) FROM community.post_comments)||' likes='||(SELECT count(*) FROM community.post_likes)||' follows='||(SELECT count(*) FROM social.user_follows)||' reactions='||(SELECT count(*) FROM preference.user_place_reactions)||' saved='||(SELECT count(*) FROM preference.user_saved_places)||' votes='||(SELECT count(*) FROM voting.vote_sessions)||' media='||(SELECT count(*) FROM media.media_files)||' notifications='||(SELECT count(*) FROM notification.notifications)||' ai_msgs='||(SELECT count(*) FROM ai.ai_chat_messages);`);
  log('DONE', summary[0][0]);
  log(`manifest: ${manifest.length} uploads → seeds/v2/media-manifest-v2.csv`);
}
main().catch((e) => { console.error('\nFAILED:', e.message); process.exit(1); });
