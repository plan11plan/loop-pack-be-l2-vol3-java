## 테스트 데이터 설계

이 프로젝트의 데이터는 실제 이커머스 트래픽 패턴을 재현하도록 설계되었다. 균등 분포로 생성하면 프로덕션과 다른 실행 계획이 나와 인덱스 효과와 쿼리 병목을 제대로 측정할 수 없다.

### 분포 설계 근거

사용된 분포 용어는 다음과 같다.

- **Power-law** — 소수의 항목이 대부분의 값을 차지하는 분포. 예) 상위 10개 브랜드가 전체 상품의 절반 이상을 보유
- **Zipf** — Power-law의 극단적인 형태. 순위가 낮아질수록 빈도가 급격히 감소한다. 예) 1위 상품이 2위보다 2배, 3위보다 3배 많은 좋아요를 받음

| 테이블 | 건수 | 분포 | 선택 이유 |
|---|---|---|---|
| brands | 100 | 균등 | — |
| products | 100,000 | Power-law | 브랜드별 상품 수 편중 → 브랜드 필터 쿼리의 인덱스 선택성이 실제와 같아짐 |
| users | 10,000 | 균등 | 유저 간 편차보다 상품 간 인기 편차가 핵심 변수 |
| likes | ~416,000 | Zipf | 상위 1% 상품에 ~70% 집중 → `ORDER BY like_count DESC` 정렬 및 캐시 히트율 차이 측정 가능 |
| orders | 100,000 | Power-law | 인기 상품(좋아요 상위)에 집중 → 특정 상품 주문 이력 페이지네이션 병목 재현 |
| order_items | ~200,000 | 주문당 1~3개 | — |

### 설정 (application.yml)

건수는 아래 값을 변경해 조정할 수 있다. 기본값은 비활성화(`enabled: false`)이다.
```yaml
app:
  data-generator:
    enabled: false
    brand-count: 100
    product-count: 100000
    user-count: 10000
    like-count: 500000
    order-count: 100000
```

### 실행

두 가지 방법으로 데이터를 생성할 수 있다.

**방법 1: 서버 기동 시 자동 생성**
```bash
./gradlew :apps:commerce-api:bootRun --args='--app.data-generator.enabled=true'
```

**방법 2: 서버 기동 후 API 호출**
```bash
# 데이터 생성 요청 (비동기, 즉시 반환)
curl -X POST -H "X-Loopers-Ldap: loopers.admin" http://localhost:8080/api-admin/v1/data-generator/bulk-init
```
```bash
# 생성 현황 확인
curl -H "X-Loopers-Ldap: loopers.admin" http://localhost:8080/api-admin/v1/data-generator/stats
```
