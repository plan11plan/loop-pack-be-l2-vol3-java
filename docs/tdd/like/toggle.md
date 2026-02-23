# TDD: UC-L01 좋아요 토글 (등록/취소)

| 항목 | 내용 |
|------|------|
| 도메인 | like |
| 상태 | 🟡 진행 중 |
| DESIGN.md | docs/design/like/DESIGN.md |

---

## 테스트 목록표

| Round | 계층 | 테스트 대상 | 테스트 케이스 |
|-------|------|------------|-------------|
| R1 | Domain | ProductLikeModelTest | 유효한 userId와 productId가 주어지면, 정상적으로 생성된다 |
| R2 | Domain | ProductLikeModelTest | userId가 null이면 예외가 발생한다 |
| R3 | Domain | ProductLikeModelTest | productId가 null이면 예외가 발생한다 |
| R4 | Domain | ProductLikeServiceTest | 좋아요가 없으면, 좋아요를 등록하고 true를 반환한다 |
| R5 | Domain | ProductLikeServiceTest | 좋아요가 이미 존재하면, 좋아요를 삭제하고 false를 반환한다 |
| R6 | Application | LikeFacadeTest | 상품을 검증하고 ProductLikeService.toggleLike를 호출한다 |
| R7 | Application | LikeFacadeTest | 좋아요가 등록되면 product.addLikeCount()를 호출한다 |
| R8 | Application | LikeFacadeTest | 좋아요가 취소되면 product.subtractLikeCount()를 호출한다 |

---

## Round 진행 현황

### Round 1: 유효한 userId와 productId가 주어지면, 정상적으로 생성된다
- 🔴 Red: ✅ 실패 확인 — ProductLikeModel 클래스 미존재로 컴파일 실패
- 🟢 Green: ✅ 통과 — ProductLikeModel Entity 생성 (standalone, BaseEntity 미상속)
- 🔵 Refactor: skip

### Round 2: userId가 null이면 예외가 발생한다
- 🔴 Red: ✅ 실패 확인 — 검증 없이 생성되어 예외 미발생
- 🟢 Green: ✅ 통과 — validateUserId() 추가
- 🔵 Refactor: skip

### Round 3: productId가 null이면 예외가 발생한다
- 🔴 Red: ✅ 실패 확인 — productId 검증 미존재
- 🟢 Green: ✅ 통과 — validateProductId() 추가
- 🔵 Refactor: skip

### Round 4: 좋아요가 없으면, 좋아요를 등록하고 true를 반환한다
- 🔴 Red: ✅ 실패 확인 — toggleLike() 메서드 미존재, 컴파일 실패
- 🟢 Green: ⏳ 대기
- 🔵 Refactor: ⏳ 대기

### Round 5: 좋아요가 이미 존재하면, 좋아요를 삭제하고 false를 반환한다
- 🔴 Red: ⏳ 대기
- 🟢 Green: ⏳ 대기
- 🔵 Refactor: ⏳ 대기

### Round 6: 상품을 검증하고 ProductLikeService.toggleLike를 호출한다
- 🔴 Red: ⏳ 대기
- 🟢 Green: ⏳ 대기
- 🔵 Refactor: ⏳ 대기

### Round 7: 좋아요가 등록되면 product.addLikeCount()를 호출한다
- 🔴 Red: ⏳ 대기
- 🟢 Green: ⏳ 대기
- 🔵 Refactor: ⏳ 대기

### Round 8: 좋아요가 취소되면 product.subtractLikeCount()를 호출한다
- 🔴 Red: ⏳ 대기
- 🟢 Green: ⏳ 대기
- 🔵 Refactor: ⏳ 대기

---

## 전체 테스트 결과

(완료 후 기록)

---

## 산출물

(완료 후 기록)
