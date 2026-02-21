# 공통 설계 원칙

## 프로젝트 개요

SSENSE와 같은 하이패션 이커머스 플랫폼. Spring Boot 3.4.4, Java 21.

### 액터

| 액터 | 설명 | 식별 방식 |
|------|------|----------|
| 비회원 (Guest) | 로그인하지 않은 사용자 | 헤더 없음 |
| 회원 (User) | 로그인한 사용자 | `X-Loopers-LoginId`, `X-Loopers-LoginPw` |
| 관리자 (Admin) | 사내 관리자 | `X-Loopers-Ldap: loopers.admin` |

### API Prefix

- 대고객 API: `/api/v1`
- 어드민 API: `/api-admin/v1`

---

## 도메인 참조 원칙

- **DB FK 제약 미사용** — 테이블 간 외래키 제약조건을 사용하지 않는다. 무결성은 애플리케이션 레벨에서 보장.
  - FK의 문제: 잠금 전파(데드락 위험), 삭제 순서 강제, 테이블 간 결합
- **DB 유니크 제약 사용** — 테이블 내부 제약은 사용한다. 동시성(더블클릭 등) 시 중복 방지.
- **참조 방식**
  - 같은 도메인 (Brand → Product): 객체참조 + FK 없음 (`@ManyToOne` + `ConstraintMode.NO_CONSTRAINT`)
  - 다른 도메인 간: ID 참조 (`private Long userId` 등)
- **Aggregate** — 각 도메인은 독립 Aggregate Root. `@OneToMany` 사용하지 않음. Aggregate 규칙은 Service에서 `@Transactional`로 관리.

---

## Soft Delete 전략

- **Soft Delete 대상**: brands, products, orders, order_items → `deleted_at` 컬럼으로 논리 삭제
- **Hard Delete 대상**: likes, cart_items → 이력이 필요 없는 토글/임시 데이터. UNIQUE 제약조건과의 충돌 방지.

---

## 공통 엔티티 구조

- **BaseEntity**: 공통 컬럼 (id, created_at, updated_at, deleted_at)
- **Enum 저장**: VARCHAR로 저장
- **Rich Domain Model**: 비즈니스 로직은 엔티티와 VO 메서드에 포함. Facade는 오케스트레이션만 담당.

---

## 도메인 용어집

| 한글 | 영문 | 설명 |
|------|------|------|
| 회원 | User | 서비스에 가입한 사용자. 구현 완료 (범위 제외) |
| 브랜드 | Brand | 상품을 판매하는 브랜드. Admin이 등록/관리 |
| 상품 | Product | 브랜드에 속한 판매 상품. 재고(stock) 포함 |
| 좋아요 | Like | 회원이 상품에 대해 표현하는 선호. 회원당 상품당 1개 |
| 장바구니 항목 | CartItem | 장바구니에 담긴 개별 상품과 수량 |
| 주문 | Order | 회원이 상품을 구매하기 위한 요청 |
| 주문 항목 | OrderItem | 주문에 포함된 개별 상품의 스냅샷 |
| 스냅샷 | Snapshot | 주문 시점의 상품 정보를 복사하여 저장하는 것 |

---

## 범위 제외 사항

| 제외 항목 | 사유 |
|---|---|
| 유저(Users) 기능 | 회원가입, 내 정보 조회, 비밀번호 변경은 이미 구현 완료 |
| 결제(Payment) | 향후 별도 단계에서 추가 개발 예정 |
| 쿠폰(Coupon) | 향후 별도 단계에서 추가 개발 예정 |
| 주문 상태 전이 | 결제 기능과 함께 추가. 현재는 주문 생성(ORDERED)만 |
| 주문 취소 | 현재 범위에서 제공하지 않음 |
