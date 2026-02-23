# UC-L01: 좋아요 토글 (등록/취소)

> DESIGN.md: `docs/design/like/DESIGN.md`

---

## Domain — Entity

### ProductLikeModel 생성
- [x] 🔴 Red: 유효한 userId와 productId가 주어지면, 정상적으로 생성된다
- [x] 🟢 Green
- [x] 🔵 Refactor: skip

### ProductLikeModel userId 검증
- [x] 🔴 Red: userId가 null이면 예외가 발생한다
- [x] 🟢 Green
- [x] 🔵 Refactor: validate() 하나로 통합

### ProductLikeModel productId 검증
- [x] 🔴 Red: productId가 null이면 예외가 발생한다
- [x] 🟢 Green
- [x] 🔵 Refactor: skip

---

## Domain — Service

### 좋아요 등록 (toggle → true)
- [x] 🔴 Red: 좋아요가 없으면, 좋아요를 등록하고 true를 반환한다
- [ ] 🟢 Green
- [ ] 🔵 Refactor

### 좋아요 취소 (toggle → false)
- [x] 🔴 Red: 좋아요가 이미 존재하면, 좋아요를 삭제하고 false를 반환한다
- [ ] 🟢 Green
- [ ] 🔵 Refactor

---

## Application — Facade

### 상품 검증 + toggleLike 호출
- [ ] 🔴 Red: 상품을 검증하고 ProductLikeService.toggleLike를 호출한다
- [ ] 🟢 Green
- [ ] 🔵 Refactor

### 좋아요 등록 시 likeCount 증가
- [ ] 🔴 Red: 좋아요가 등록되면 product.addLikeCount()를 호출한다
- [ ] 🟢 Green
- [ ] 🔵 Refactor

### 좋아요 취소 시 likeCount 감소
- [ ] 🔴 Red: 좋아요가 취소되면 product.subtractLikeCount()를 호출한다
- [ ] 🟢 Green
- [ ] 🔵 Refactor
