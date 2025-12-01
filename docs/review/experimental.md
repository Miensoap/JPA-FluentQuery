# 기능 분류: Spring Data JPA 기반 vs 실험적 기능

## Spring Data JPA 위에서 동작하는 핵심 기능
- **레포지토리 통합 진입점**: `FluentRepository`가 `JpaRepository`와 `JpaSpecificationExecutor`를 모두 상속해 기존 CRUD/Specification 기반 기능 위에 DSL 엔트리 포인트(`query()`)를 노출합니다.【F:src/main/java/me/miensoap/fluent/FluentRepository.java†L3-L16】
- **Specification 조합형 DSL**: `FluentQuery`가 `Specification.where/and/or`를 사용해 조건을 누적하고, `findAll/findOne/count/exists` 같은 표준 Spring Data JPA 메서드에 그대로 위임합니다. `Sort`와 `Pageable`을 받아 기존 정렬·페이징 기능도 재사용합니다.【F:src/main/java/me/miensoap/fluent/core/FluentQuery.java†L23-L146】
- **필드 단위 조건식 매핑**: `FieldStep`이 `CriteriaBuilder` 연산(`equal`, `greaterThan`, `between`, `like`, `in` 등)을 그대로 호출해 문자열/숫자/날짜/컬렉션/불리언 조건을 `Specification`으로 변환합니다.【F:src/main/java/me/miensoap/fluent/core/FieldStep.java†L14-L191】
- **정렬 DSL**: `orderBy(...).ascending()/descending()` 체인이 내부 `Sort.Order`를 쌓아 `findAll(spec, sort)` 호출로 연결되며, 기존 정렬 처리 위에서 작동합니다.【F:src/main/java/me/miensoap/fluent/core/FluentQuery.java†L104-L126】【F:src/main/java/me/miensoap/fluent/core/OrderStep.java†L6-L19】

## 비교적 안정적인 부가 기능
- **메서드 참조 기반 타입 안전성**: `Property` + `PropertyNameResolver`가 getter 메서드 참조에서 필드명과 반환 타입을 추출하고, `FieldStep`이 타입에 맞지 않는 연산 시 명시적 예외를 던집니다. 문자열 입력 대신 컴파일 시점에 가까운 검증을 제공하는 실험적 안전장치입니다.【F:src/main/java/me/miensoap/fluent/core/PropertyNameResolver.java†L14-L78】【F:src/main/java/me/miensoap/fluent/core/FieldStep.java†L16-L256】

## 새로운 실험적/확장 기능
- **Fetch Join DSL**: `fetchJoin` 체인이 `FetchJoinDescriptor`를 통해 중첩 경로를 파싱하고, 중복 경로를 제거한 뒤 Criteria 트리에 fetch join을 적용합니다. Count 쿼리에서는 fetch join을 건너뛰도록 분기해 실험적 fetch join 지원을 제공합니다.【F:src/main/java/me/miensoap/fluent/core/FluentQuery.java†L87-L189】【F:src/main/java/me/miensoap/fluent/core/FetchJoinDescriptor.java†L16-L74】
- **쿼리 레벨 옵션 확장**: `not()`과 `distinct()`가 누적된 `Specification`을 감싸거나 `CriteriaQuery`에 `distinct(true)`를 설정해 표준 파생 메서드에 없는 전역 제어를 제공합니다.【F:src/main/java/me/miensoap/fluent/core/FluentQuery.java†L75-L173】
- **빈 컬렉션/널 입력 처리 규칙**: `in`, `notIn`, 문자열 연산 등에서 널/빈 값을 명시적으로 검증하여 의도한 논리식(`disjunction`, `conjunction` 등)을 반환합니다. 표준 파생 메서드와 달리 입력 검증 로직을 DSL 차원에서 실험적으로 정의한 부분입니다.【F:src/main/java/me/miensoap/fluent/core/FieldStep.java†L149-L226】
