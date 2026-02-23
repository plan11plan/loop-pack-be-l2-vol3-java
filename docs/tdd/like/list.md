# UC-L02: 좋아요 목록 조회

> DESIGN.md: `docs/design/like/DESIGN.md`

---

## Domain — Service

### 사용자별 좋아요 목록 조회
- [x] 🔴 Red: 사용자 ID로 좋아요 목록을 조회하면, 해당 사용자의 좋아요 목록이 반환된다
- [ ] 🟢 Green
- [ ] 🔵 Refactor

### 좋아요 없는 사용자 조회
- [x] 🔴 Red: 좋아요가 없는 사용자 ID로 조회하면, 빈 목록이 반환된다
- [ ] 🟢 Green
- [ ] 🔵 Refactor

---

## Application — Facade

### 좋아요 목록을 LikeInfo로 반환
- [ ] 🔴 Red: 사용자의 좋아요 목록을 조회하면, LikeInfo 목록을 반환한다
- [ ] 🟢 Green
- [ ] 🔵 Refactor

### 삭제된 상품 필터링
- [ ] 🔴 Red: 삭제된 상품의 좋아요는 목록에서 제외된다
- [ ] 🟢 Green
- [ ] 🔵 Refactor
