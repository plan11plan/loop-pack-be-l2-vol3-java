## 프로젝트 개요

Spring Boot 기반 멀티모듈 Java 프로젝트. TDD 방식으로 개발하며, 테스트 가능한 구조를 목표로 한다.

---

## 기술 스택 및 버전

### Core
- **Java**: 21
- **Spring Boot**: 3.4.4
- **Spring Cloud**: 2024.0.1
- **Gradle**: Kotlin DSL

### Framework & Libraries
- **Spring Web**: REST API 개발
- **Spring Data JPA**: 데이터 접근 계층
- **Spring Data Redis**: 캐싱 및 세션 관리
- **QueryDSL**: 타입 세이프한 쿼리 작성
- **Kafka**: 메시지 브로커 (commerce-streamer)
- **Spring Batch**: 배치 처리 (commerce-batch)

### Utilities
- **Lombok**: 보일러플레이트 코드 감소
- **Jackson**: JSON 직렬화/역직렬화
- **SpringDoc OpenAPI**: API 문서화 (Swagger)

### Testing
- **JUnit 5 + AssertJ + Mockito**: 테스트 프레임워크
- **Testcontainers**: 통합 테스트 (MySQL, Redis)

### Monitoring & Logging
- **Spring Actuator**: 애플리케이션 모니터링
- **Slack Appender**: 로그 알림
- **Jacoco**: 코드 커버리지

---
## 모듈 구조
```
apps/
├── commerce-api          # REST API 서버
├── commerce-streamer     # Kafka 스트리밍 처리
└── commerce-batch        # 배치 작업

modules/
├── jpa                   # BaseEntity, QueryDSL/JPA/DataSource Config
├── redis                 # Redis 설정 및 Repository
└── kafka                 # Kafka 설정 및 Producer/Consumer

supports/
├── jackson               # JSON 직렬화 설정
├── logging               # 로깅 설정
└── monitoring            # 모니터링 설정
```
---

## 아키텍처

계층 우선 패키지: `interfaces → application → domain ← infrastructure`

### 컨벤션

- 코딩 컨벤션: `.claude/skills/project-convention/` 참조 (코드 작성 시 해당 스킬의 references/ 하위 문서를 반드시 Read 도구로 읽을 것)
- 커밋 규칙: `.claude/skills/commit-convention/` 참조

### 설계 문서

기능 개발 시 해당 도메인의 설계 문서를 **먼저 읽고** 시작한다.

| 문서 | 경로 | 용도 |
|------|------|------|
| 공통 원칙 | `docs/spec/shared/CONVENTIONS.md` | 참조 방식, Soft Delete, 용어집 등 |
| 전체 구조 | `docs/spec/shared/OVERVIEW.md` | 전체 ERD + 클래스 다이어그램 |
| 도메인 스펙 | `docs/spec/{domain}/DESIGN.md` | 요구사항 + 유즈케이스 + 시퀀스 + ERD + 클래스 |

도메인: `brand`, `product`, `like`, `cart`, `order`

**참조 규칙:**
1. 해당 도메인의 `DESIGN.md`를 읽는다
2. 다른 도메인과 연동이 필요하면 그 도메인의 `DESIGN.md`도 읽는다
3. 전체 관계 확인이 필요하면 `OVERVIEW.md`를 읽는다

---

## 프로젝트 실행

```bash
# 개발 환경
./gradlew :apps:commerce-api:bootRun

# 테스트
./gradlew test                          # 전체
./gradlew :apps:commerce-api:test       # 특정 모듈
./gradlew test jacocoTestReport         # 커버리지

# 인프라
docker compose up -d
```
