# Infrastructure 계층 컨벤션

## 목차

1. [Repository 패턴](#1-repository-패턴)
2. [QueryDSL 규칙](#2-querydsl-규칙)
3. [BaseEntity](#3-baseentity)
4. [DB 제약조건 규칙](#4-db-제약조건-규칙)
5. [멀티 모듈 구조](#5-멀티-모듈-구조)
6. [체크리스트](#체크리스트)

---

## 1. Repository 패턴

### 3-클래스 패턴

Repository는 **domain 인터페이스 + infrastructure 구현체 + JpaRepository** 3개로 구성한다.

```
domain/
└── order/
    └── OrderRepository.java            ← 순수 인터페이스 (Spring 의존 없음)

infrastructure/
└── order/
    ├── OrderJpaRepository.java         ← Spring Data JPA 인터페이스
    └── OrderRepositoryImpl.java        ← 어댑터: OrderRepository 구현, JpaRepository에 위임
```

왜 3-클래스인가:
- **domain이 Spring을 모른다** — `OrderRepository`는 순수 Java 인터페이스. JPA/Spring Data 의존이 없어서 domain 계층의 순수성이 보장된다
- **테스트가 쉽다** — domain 단위 테스트에서 `OrderRepository`의 Fake를 만들면 DB 없이 테스트 가능
- **구현 교체가 자유롭다** — JPA에서 다른 저장소로 바꿔도 domain은 변경 불필요

### domain Repository 인터페이스

Spring 의존 없는 **순수 Java 인터페이스**로 작성한다. domain이 필요로 하는 메서드만 선언한다.

```java
// domain/order/OrderRepository.java
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    List<Order> findByUserId(Long userId);
}
```

**금지:**
- `JpaRepository` 상속
- Spring 어노테이션 (`@Repository`, `@Query` 등)
- `Pageable`, `Page` 등 Spring Data 타입

### JpaRepository 인터페이스

Spring Data JPA의 자동 구현을 활용하는 인터페이스. infrastructure에 배치한다.

```java
// infrastructure/order/OrderJpaRepository.java
public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    Optional<Order> findByIdAndDeletedAtIsNull(Long id);
}
```

### RepositoryImpl — 어댑터

domain Repository를 구현하고, JpaRepository에 위임한다. `@Repository`를 사용한다.

```java
// infrastructure/order/OrderRepositoryImpl.java
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return orderJpaRepository.findByUserId(userId);
    }
}
```

`@Repository`를 사용하는 이유:
- **의미론적 명확성** — 이 클래스가 데이터 접근 계층임을 명시한다
- **영속성 예외 변환** — JPA 벤더별 예외를 Spring `DataAccessException`으로 자동 변환한다
- **Spring 스테레오타입 관례** — `@Controller`/`@Service`/`@Repository`는 계층별 표준 어노테이션이다

### Soft Delete 조회 처리

soft delete된 엔티티 필터링은 **RepositoryImpl에서만 처리**한다. Domain Service에서 `isDeleted()` 등을 이중 체크하지 않는다.

**핵심 규칙: RepositoryImpl의 모든 조회 메서드는 `deletedAt IS NULL`을 기본 적용한다.**

```java
// domain 인터페이스 — soft delete를 모른다
Optional<Order> findById(Long id);
Optional<Order> findByName(String name);

// RepositoryImpl — 모든 조회에서 soft delete 필터링
@Override
public Optional<Order> findById(Long id) {
    return orderJpaRepository.findByIdAndDeletedAtIsNull(id);
}

@Override
public Optional<Order> findByName(String name) {
    return orderJpaRepository.findByNameAndDeletedAtIsNull(name);
}
```

왜 RepositoryImpl에서만 처리하는가:
- **단일 책임**: soft delete는 저장소 세부사항이므로 infrastructure가 담당한다
- **domain 순수성**: domain이 "삭제된 데이터" 개념을 알 필요 없다
- **일관성**: 모든 조회 메서드에 동일한 규칙이 적용되므로 누락 위험이 없다
- **Service 단순화**: Domain Service에서 `isDeleted()` 체크가 불필요하다

### 네이밍 규칙

| 클래스 | 네이밍 | 어노테이션 | 위치 |
|--------|--------|-----------|------|
| domain 인터페이스 | `{Domain}Repository` | 없음 | `domain/{domain}/` |
| JPA 인터페이스 | `{Domain}JpaRepository` | 없음 (자동) | `infrastructure/{domain}/` |
| 어댑터 구현체 | `{Domain}RepositoryImpl` | `@Repository` | `infrastructure/{domain}/` |

---

## 2. QueryDSL 규칙

### RepositoryImpl에 직접 작성

QueryDSL 쿼리는 **RepositoryImpl에 직접 작성**한다. RepositoryImpl이 이미 어댑터 역할을 하고 있으므로, 단순 CRUD(JpaRepository 위임)와 복잡 쿼리(QueryDSL)를 한 곳에서 관리한다.

```java
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;
    private final JPAQueryFactory queryFactory;

    // === 단순 CRUD: JpaRepository에 위임 === //

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    // === 복잡 쿼리: QueryDSL 직접 작성 === //

    @Override
    public Page<Product> search(ProductSearchCondition condition, Pageable pageable) {
        QProduct product = QProduct.product;
        QBrand brand = QBrand.brand;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(product.deletedAt.isNull());
        builder.and(brand.deletedAt.isNull());

        if (condition.brandId() != null) {
            builder.and(product.brand.id.eq(condition.brandId()));
        }

        List<Product> content = queryFactory
            .selectFrom(product)
            .join(product.brand, brand)
            .where(builder)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(buildOrderSpecifier(condition.sort(), product))
            .fetch();

        Long total = queryFactory
            .select(product.count())
            .from(product)
            .join(product.brand, brand)
            .where(builder)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
```

### 분리 시점

RepositoryImpl의 QueryDSL 메서드가 **5개 이상**이 되거나 쿼리 복잡도가 높아지면, 별도 클래스로 분리한다.

```
// 초기 — RepositoryImpl에 직접
infrastructure/
└── product/
    ├── ProductJpaRepository.java
    └── ProductRepositoryImpl.java       ← JPA 위임 + QueryDSL 모두

// 쿼리가 많아지면 — 분리
infrastructure/
└── product/
    ├── ProductJpaRepository.java
    ├── ProductQueryRepository.java      ← QueryDSL 전용 (NEW)
    └── ProductRepositoryImpl.java       ← JPA 위임 + QueryRepository에 위임
```

```java
// 분리 후 QueryRepository
@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Product> search(ProductSearchCondition condition, Pageable pageable) {
        // QueryDSL 쿼리 ...
    }
}

// 분리 후 RepositoryImpl
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;
    private final ProductQueryRepository productQueryRepository;

    @Override
    public Page<Product> search(ProductSearchCondition condition, Pageable pageable) {
        return productQueryRepository.search(condition, pageable);
    }
}
```

### 동적 정렬

정렬 조건은 **QueryDSL `OrderSpecifier`로 변환**한다. Controller에서 받은 `sort` 파라미터를 기반으로 한다.

```java
private OrderSpecifier<?> buildOrderSpecifier(String sort, QProduct product) {
    if (sort == null) return product.createdAt.desc();

    return switch (sort) {
        case "price_asc" -> product.price.asc();
        case "likes_desc" -> product.likeCount.desc();
        default -> product.createdAt.desc();  // latest
    };
}
```

### 검색 조건 DTO

QueryDSL 검색 조건은 domain 패키지에 **record**로 정의한다. 쿼리 파라미터를 담는 용도이므로 domain DTO(`~Condition`)로 둔다.

```java
// domain/product/dto/ProductSearchCondition.java
public record ProductSearchCondition(
    Long brandId,
    String sort
) {}
```

---

## 3. BaseEntity

### 구조

모든 Entity는 `BaseEntity`를 상속한다. `modules/jpa` 모듈에 위치하여 전 앱에서 재사용한다.

```java
// modules/jpa — com.loopers.domain.BaseEntity
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    protected void guard() {}            // 검증 훅 (PrePersist/PreUpdate)

    @PrePersist
    private void prePersist() { ... }    // createdAt, updatedAt 자동 설정

    @PreUpdate
    private void preUpdate() { ... }     // updatedAt 자동 갱신

    public void delete() { ... }         // 멱등 soft delete
    public void restore() { ... }        // 멱등 복원
}
```

### 제공하는 것

| 기능 | 메서드/필드 | 동작 |
|------|-----------|------|
| PK 자동 생성 | `id` (IDENTITY) | DB에서 자동 할당 |
| 생성일 자동 기록 | `createdAt` | `@PrePersist`에서 설정, 이후 변경 불가 |
| 수정일 자동 갱신 | `updatedAt` | `@PrePersist`/`@PreUpdate`에서 갱신 |
| Soft Delete | `deletedAt` + `delete()` | 멱등, null이면 삭제 안 됨 |
| 복원 | `restore()` | 멱등, `deletedAt`을 null로 |
| 검증 훅 | `guard()` | 하위 Entity가 override하여 PrePersist/PreUpdate 시 검증 |

### Entity에서의 사용

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    public static Brand create(String name) {
        Brand brand = new Brand();
        brand.name = name;
        return brand;
    }

    public void update(String name) {
        this.name = name;
    }

    // guard()를 override하여 저장 시점 검증 추가 가능
    @Override
    protected void guard() {
        if (name == null || name.isBlank()) {
            throw new CoreException(BrandErrorCode.NAME_REQUIRED);
        }
    }
}
```

### 주의사항

- Entity에서 `id`, `createdAt`, `updatedAt`, `deletedAt`을 **직접 설정하지 않는다** — BaseEntity가 관리
- `delete()`는 BaseEntity의 메서드를 그대로 사용한다 — 도메인별 삭제 로직은 Service에서 조율
- 정적 팩토리 메서드에서 `id`를 파라미터로 받지 않는다 — DB가 할당

---

## 4. DB 제약조건 규칙

### FK 제약 미사용

테이블 간 외래키 제약조건을 **사용하지 않는다**. 무결성은 애플리케이션 레벨에서 보장한다.

FK를 쓰지 않는 이유:
- 잠금 전파로 인한 데드락 위험
- 삭제 순서 강제로 운영 복잡도 증가
- 테이블 간 결합으로 독립 배포/마이그레이션 어려움

### 참조 방식

| 관계 | 참조 방식 | 예시 |
|------|----------|------|
| **같은 도메인** (Brand → Product) | 객체참조 + FK 없음 | `@ManyToOne` + `NO_CONSTRAINT` |
| **다른 도메인** 간 | ID 참조 | `private Long userId` |

```java
// 같은 도메인: 객체참조 (Brand → Product는 같은 상품 도메인)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(
    name = "brand_id",
    foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
)
private Brand brand;

// 다른 도메인: ID 참조 (Order → User는 다른 도메인)
@Column(name = "user_id", nullable = false)
private Long userId;
```

### @OneToMany 미사용

`@OneToMany`를 사용하지 않는다. 하위 엔티티는 ID로 참조하고, 조회는 별도 Repository로 한다.

```java
// ❌ @OneToMany 사용
@OneToMany(mappedBy = "order")
private List<OrderItem> orderItems;

// ✅ ID 참조 + 별도 조회
// Order에는 orderItems 필드 없음
// OrderItem에 orderId 필드
@Column(name = "order_id", nullable = false)
private Long orderId;

// 조회는 Service/Repository에서
List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
```

### 유니크 제약 사용

테이블 **내부** 유니크 제약은 사용한다. 동시성(더블클릭 등) 상황에서 중복을 방지한다.

```java
// 단일 컬럼 유니크
@Column(nullable = false, unique = true)
private String name;

// 복합 유니크
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
public class Like extends BaseEntity { ... }
```

---

## 5. 멀티 모듈 구조

### 현재 구조

```
loop-pack-be-l2-vol3-java/
├── apps/
│   ├── commerce-api/           ← 메인 API 앱
│   ├── commerce-streamer/      ← Kafka 컨슈머
│   └── commerce-batch/         ← 배치
└── modules/
    ├── jpa/                    ← BaseEntity, QueryDSL/JPA/DataSource Config
    ├── kafka/
    └── redis/
```

### 모듈 간 역할 분담

| 위치 | 포함하는 것 |
|------|-----------|
| `modules/jpa` | `BaseEntity`, `QueryDslConfig`, `JpaConfig`, `DataSourceConfig` |
| `apps/commerce-api` | 도메인 코드, Controller, Facade, Service, Entity, Repository, Infrastructure |

### 패키지 배치 원칙

- `modules/jpa`의 BaseEntity는 `com.loopers.domain` 패키지에 위치 — 앱의 Entity가 자연스럽게 상속
- Config 클래스는 `com.loopers.config.jpa` 패키지에 위치 — 앱의 `support/config`와 분리
- **도메인 로직은 modules에 넣지 않는다** — modules는 기술 인프라만 제공

---

## 체크리스트

**Repository 패턴**
- [ ] domain Repository가 순수 Java 인터페이스인가? (Spring 의존 없음)
- [ ] JpaRepository가 infrastructure에 있는가?
- [ ] RepositoryImpl에 `@Repository`가 붙어 있는가?
- [ ] RepositoryImpl이 domain Repository를 implements하는가?
- [ ] soft delete 필터링이 RepositoryImpl에서 처리되는가?

**QueryDSL**
- [ ] QueryDSL 쿼리가 RepositoryImpl에 작성되어 있는가? (또는 분리 시 QueryRepository)
- [ ] JPAQueryFactory를 생성자 주입으로 받는가?
- [ ] 동적 정렬이 OrderSpecifier로 처리되는가?

**BaseEntity**
- [ ] 모든 Entity가 BaseEntity를 상속하는가?
- [ ] Entity에서 id, createdAt, updatedAt, deletedAt을 직접 설정하지 않는가?
- [ ] 저장 시점 검증이 필요하면 guard()를 override하는가?

**DB 제약조건**
- [ ] FK 제약조건을 사용하지 않는가? (NO_CONSTRAINT)
- [ ] 같은 도메인은 객체참조, 다른 도메인은 ID 참조인가?
- [ ] @OneToMany를 사용하지 않는가?
- [ ] 유니크 제약이 필요한 곳에 적용되어 있는가?
