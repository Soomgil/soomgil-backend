# 숨길 Backend

숨길의 Spring Boot API 서버입니다. 여행방 협업, 취향 기반 장소 추천, 일정 편집, 기록, 커뮤니티 기능을 제공하고 프론트엔드가 사용하는 REST/WebSocket API를 담당합니다.

## 서비스 도메인

| 도메인 | 설명 |
| :--- | :--- |
| Auth/User | 회원가입, 로그인, OAuth, 내 프로필과 설정 |
| Trip | 여행방 생성, 멤버 관리, 초대 링크, 권한 확인 |
| Itinerary | 일차/일정/경로/지도 그림 관리 |
| Preference/Swipe | 장소 스와이프, 슈퍼라이크, 사용자 취향 projection |
| Place/Search | 장소 검색, 접근성 정보, 추천 후보 조회 |
| AI | 여행방 상태를 참고하는 AI 가이드와 제한된 도구 실행 |
| Chat/Notification | 여행방 채팅, 초대 알림 |
| Record/Community | 여행 기록 미디어, 여행기 발행, 댓글, 좋아요, 리트립 |

## 기술 스택

- Java 21
- Spring Boot 3
- Gradle
- PostgreSQL
- Redis
- Flyway
- MyBatis
- Spring Security
- Spring Modulith
- MinIO/S3

## 실행

JDK 21과 Docker 실행 환경이 필요합니다.

```bash
docker compose up -d
./gradlew bootRun
```

Windows PowerShell:

```powershell
docker compose up -d
.\gradlew.bat bootRun
```

기본 API 서버 주소는 http://localhost:8080 입니다.

## 주요 명령

| 명령 | 설명 |
| :--- | :--- |
| `./gradlew bootRun` | 로컬 서버 실행 |
| `./gradlew test` | 테스트 실행 |
| `./gradlew build` | 빌드 |
| `docker compose up -d` | PostgreSQL, Redis, Mailpit, MinIO 실행 |
| `docker compose down` | 로컬 인프라 종료 |

## 주요 엔드포인트

| 엔드포인트 | 설명 |
| :--- | :--- |
| `/api/v1/health` | 서비스 헬스 체크 |
| `/actuator/health` | Actuator 헬스 체크 |
| `/swagger-ui` | Swagger UI |

로컬 보조 서비스:

| 서비스 | 주소 |
| :--- | :--- |
| Mailpit | http://localhost:8025 |
| MinIO Console | http://localhost:9001 |

## 구조

도메인은 CQRS-lite 형태로 구성합니다.

```text
{domain}/
  api/
  application/
    command/
    query/
  domain/
  infrastructure/
```

기본 원칙:

- Controller는 command/query handler를 호출합니다.
- Command는 상태 변경, Query는 조회 책임을 가집니다.
- DB 접근은 MyBatis mapper와 repository에서 처리합니다.
- JPA/Hibernate는 사용하지 않습니다.

## 환경변수

로컬 통합 실행은 루트 `soomgil/.env`와 `compose.yaml` 기준으로 관리합니다. 실제 비밀값은 커밋하지 않습니다.
