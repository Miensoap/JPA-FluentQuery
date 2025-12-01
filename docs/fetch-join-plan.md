# Fetch Join 지원 계획

## 현황 분석
- `FluentQuery` (`src/main/java/me/miensoap/fluent/FluentQuery.java`) 는 `Specification`/`Sort` 조합만 다루며, `fetch join` 관련 필드나 API 가 없습니다.
- 경로 탐색은 `FieldStep#path` 로 단순히 `root.get("team").get("members")` 형태만 지원하며, `root.fetch(...)` 호출은 전혀 없습니다.
- 테스트/문서 어디에도 `fetch join` 이라는 개념이 등장하지 않아 N+1 대응 수단이 없습니다.

## 목표
1. 플루언트 DSL 에서 `fetchJoin(Member::getTeam)` 혹은 `fetchJoin("team", JoinType.LEFT)` 처럼 연관 엔티티를 즉시로딩하도록 지시할 수 있어야 합니다.
2. 다중 fetch join 및 중첩 경로(`team.company`)도 지정 가능해야 합니다.
3. 기존 where/order/distinct DSL 과 동시에 동작해야 하며, fetch join 으로 인해 중복이 생길 경우 `distinct()` 와 쉽게 조합되도록 설계합니다.
4. 기능 추가 후 README 와 테스트에 사용 예제를 추가합니다.

## 구현 단계
1. **페치 조인 메타데이터 정의**
   - `me.miensoap.fluent` 패키지에 `FetchJoinSpec` (path, joinType, optionalFetchGraph?) 클래스를 추가합니다.
   - 기본값은 `JoinType.LEFT`, 사용자가 명시하면 덮어쓰도록 합니다.
2. **FluentQuery 확장**
   - `FluentQuery` 에 `List<FetchJoinSpec> fetchJoins` 필드를 도입하고 `fetchJoin(...)` API 들을 추가합니다.
   - (a) 문자열 기반 `fetchJoin(String path)` / `fetchJoin(String path, JoinType joinType)`
   - (b) 메서드 레퍼런스 기반 `fetchJoin(Property<T, ?> property)` / `fetchJoin(Property<T, ?> property, JoinType joinType)`
   - `currentSpec()` 에서 기존 `distinct` 래퍼 로직을 일반화하여 `applyQueryCustomizers` 메서드로 추출한 뒤, 지정된 fetch join 들을 `root.fetch(...)` 체인으로 적용합니다. 중첩 경로는 `FetchParent` 를 따라가며 생성합니다.
   - 다중 호출 시 같은 경로를 중복 생성하지 않도록 `Map` 으로 dedupe 하거나 `FetchJoinSpec#apply` 내에서 이미 존재하는 fetch 를 재사용합니다.
3. **FieldStep 와 상호작용**
   - 필요 시 `FieldStep` 에 별도 변경은 없으나, nested path 탐색 로직(`field.split(".")`)을 `FetchJoinSpec` 에 재사용할 수 있도록 `PathResolver` 유틸을 추출하면 중복을 줄일 수 있습니다.
4. **문서/샘플 코드 업데이트**
   - `README.md` 에 fetch join 사용 시그니처와 제약(컬렉션 fetch join + 페이징 주의)을 설명하는 섹션 추가.
   - `enhance.md` 또는 새 `fetch-join.md` 에 디자인 결정 배경 기록.
5. **테스트 추가**
   - `FluentQueryFetchJoinTest` (통합 테스트) 신설: `Member` ↔ `Team` ↔ `Company` 처럼 연관을 구성하고, fetch join 호출 시 `Hibernate` 의 `Statistics#getFetchCount` 또는 `EntityManagerFactory#getPersistenceUnitUtil()` 로 lazy 로딩 여부 검증.
   - 페이징 시 단일 엔티티 fetch join 이 허용되는지 확인, 컬렉션 fetch join + distinct 조합이 중복 제거하는지 검증.
   - 스펙 조합 시 fetch join 도 함께 적용되는지 (e.g. `where(...)` + `fetchJoin(...)` → eager load) 확인.

## 리스크 및 대응
- **컬렉션 fetch join + 페이징**: Hibernate 가 경고하므로 README 에 강조하고, 테스트로 케이스를 커버합니다.
- **중복 Fetch 생성**: 경로 기반 dedupe 구현으로 해결; path 파싱 유틸을 별도로 검증합니다.
- **API 범람**: method reference + 문자열 버전 두 가지만 노출해 DSL 단순성 유지합니다.

## 완료 조건
- 새로운 fetch join API 가 자바독/README 로 문서화되고, 통합 테스트가 통과합니다.
- 기존 기능 (정렬/페이징/where) 회귀 테스트가 모두 성공합니다.
