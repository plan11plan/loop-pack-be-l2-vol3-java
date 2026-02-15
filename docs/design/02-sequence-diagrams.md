

##  주문 요청

### 왜 이 다이어그램이 필요한가

주문은 이 서비스에서 가장 복잡한 흐름입니다.
**Product 도메인 (상품 검증 + 재고 차감) + Order 도메인 (주문 생성 + 스냅샷)**을 조율해야 하므로 Facade가 필요합니다.
이 다이어그램으로 **트랜잭션 경계**와 **도메인 간 협력 구조**를 검증합니다.

### 다이어그램

```mermaid
sequenceDiagram
    participant OC as OrderController
    participant OF as OrderFacade
    participant PS as ProductService
    participant OS as OrderService
    participant OR as OrderRepository

    Note left of OC: POST /api/v1/orders
    OC->>OF: 주문 요청
    OF->>PS: 상품 조회 및 검증
    PS-->>OF: 상품

    Note over PS: 상품 예외 처리<br/>(없음, 삭제됨, 재고 부족)

    OF->>PS: 재고 차감
    PS-->>OF: 완료

    OF->>OS: 주문 생성 (스냅샷 포함)
    OS->>OR: 주문 저장
    OR-->>OS: 주문
    OS-->>OF: 주문
    OF-->>OC: 주문 생성 완료
```


---

## 좋아요 등록/취소

### 왜 이 다이어그램이 필요한가

좋아요는 **Product 검증 + Like 등록/취소**를 조율해야 하므로 Facade가 필요합니다.
POST/DELETE 엔드포인트는 분리하되, **내부적으로 같은 toggleLike 메서드**를 호출합니다.
이 다이어그램으로 **상품 검증 후 Facade의 토글 분기**를 검증합니다.

### 다이어그램

```mermaid
sequenceDiagram
    autonumber

    participant LC as LikeController
    participant LF as LikeFacade
    participant PS as ProductService
    participant LS as LikeService

    Note left of LC: POST /products/{id}/likes<br/>DELETE /products/{id}/likes

    LC->>LF: 좋아요 토글 요청

    Note over LF: @Transactional
    LF->>PS: 상품 검증
    activate PS
    PS-->>PS: 상품 예외처리
    PS-->>LF: 검증 완료
    deactivate PS

    LF->>LS: 좋아요 존재 확인

    alt 좋아요가 존재하지 않을 경우
        LF->>LS: save()
    else 이미 좋아요한 경우
        LF->>LS: delete()
    end
```

---

## 브랜드 삭제 (연쇄 처리)

### 왜 이 다이어그램이 필요한가

브랜드 삭제는 **Brand 삭제 + Product 연쇄 삭제**를 조율해야 하므로 Facade가 필요합니다.
단일 엔티티 삭제가 아니라, **브랜드 → 해당 브랜드의 상품 전체**를 연쇄적으로 soft delete 해야 합니다.
이 다이어그램으로 **연쇄 삭제의 범위와 순서**를 검증합니다.

### 다이어그램

```mermaid
sequenceDiagram
    participant BC as BrandAdminController
    participant BF as BrandFacade
    participant BS as BrandService
    participant PS as ProductService

    Note left of BC: DELETE /api-admin/v1/brands/{id}
    BC->>BF: 브랜드 삭제 요청
    BF->>BS: 브랜드 조회 및 검증
    BS-->>BF: 검증 완료

    Note over BS: 브랜드 예외 처리<br/>(없음, 이미 삭제됨)

    BF->>BS: 브랜드 soft delete
    BS-->>BF: 완료

    BF->>PS: 해당 브랜드 상품 전체 삭제
    PS-->>BF: 완료

    Note over BF: 장바구니/좋아요는<br/>조회 시 필터링

    BF-->>BC: 삭제 완료
```

