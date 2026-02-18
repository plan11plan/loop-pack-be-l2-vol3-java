## 프로젝트 개요

이 프로젝트는 Spring Boot 기반의 멀티모듈 Java 프로젝트입니다. TDD(Test-Driven Development) 방식으로 개발하며, 테스트 가능한 구조를 목표로 합니다.

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

### Apps (실행 가능한 애플리케이션)
```
apps/
├── commerce-api       # REST API 서버
├── commerce-streamer  # Kafka 스트리밍 처리
└── commerce-batch     # 배치 작업
```

### Modules (도메인 및 인프라 모듈)
```
modules/
├── jpa     # BaseEntity, QueryDSL/JPA/DataSource Config
├── redis   # Redis 설정 및 Repository
└── kafka   # Kafka 설정 및 Producer/Consumer
```

### Supports (공통 지원 모듈)
```
supports/
├── jackson     # JSON 직렬화 설정
├── logging     # 로깅 설정
└── monitoring  # 모니터링 설정
```

---

## 아키텍처

- 계층 우선 패키지: `interfaces → application → domain ← infrastructure`
- 코딩 컨벤션: `.claude/skills/project-convention/` 참조 (코드 작성 시 해당 스킬의 references/ 하위 문서를 반드시 Read 도구로 읽을 것)
- 커밋 규칙: `.claude/skills/commit-convention/` 참조

---

## 프로젝트 실행

### 개발 환경 실행
```bash
./gradlew :apps:commerce-api:bootRun
```

### 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :apps:commerce-api:test

# 커버리지 리포트 생성
./gradlew test jacocoTestReport
```

### Docker Compose 실행
```bash
docker compose up -d
```
