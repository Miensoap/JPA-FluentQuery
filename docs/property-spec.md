# Property 와 Specification 조합 흐름

이 문서는 `Property` 메서드 레퍼런스와 Spring Data `Specification` 이
플루언트 DSL 내부에서 어떤 역할을 하고 쿼리로 연결되는지 추적한다.

## 1. Property: 엔티티 필드를 안전하게 가리키기

- 정의: `me.miensoap.fluent.core.Property` 는 `Function<T, R>` 를
  `Serializable` 로 확장한 함수형 인터페이스다 (`core/Property.java`).
- 용도: 사용자는 `Member::getStatus` 처럼 게터 메서드 레퍼런스를 넘겨
  문자열 경로 대신 타입-안전한 필드 선택을 한다.
- 내부 처리: `PropertyNameResolver` 가 제공된 람다에서 `SerializedLambda`
  정보를 꺼내 `getStatus` → `status` 로 변환하고, 반환 타입을
  `resolveType` 으로 추출한다 (`core/PropertyNameResolver.java`).

## 2. FieldStep: Property → Specification 변환

1. `FluentQuery.where(Property)` 는 `FieldStep` 을 생성하면서 필드 경로와
   속성 타입을 보관한다 (`core/FluentQuery.java`).
2. 사용자가 `.equalTo("ACTIVE")` 같은 연산을 호출하면 `FieldStep` 은
   `Specification<T>` 람다 `(root, query, cb) -> …` 를 생성한다.
   - `root.get("status")` 처럼 경로를 계산하고
   - CriteriaBuilder (`cb`) 연산을 호출해 Predicate 를 만든다
     (`core/FieldStep.java`).
3. 생성된 Specification 은 `FluentQuery.addCondition` 으로 전달되어 현재
   빌더 상태에 `and` 또는 `or` 로 합쳐진다.
4. 타입 검증: Property 타입 메타데이터로 숫자/문자열/Boolean/Collection
   연산을 구분하고 잘못된 연산 시 명확한 예외를 던진다.

## 3. 직접 Specification 주입

- DSL 은 기존 Spring Data Specification 과도 호환되도록 `where(Spec)` /
  `and(Spec)` / `or(Spec)` 메서드를 제공한다.
- 이 경우 사용자가 작성한 Specification 이 내부 `spec` 필드에 바로
  설정되거나, 기존 조건과 합쳐진다.
- 따라서 Property 기반 조건과 수동 Specification 을 한 체인에서 섞어도
  모두 동일한 `Specification<T>` 객체에 축적된다.

## 4. FluentQuery: Specification 누적과 실행

1. `FluentQuery` 는 `JpaSpecificationExecutor` 를 받아 내부 `spec` 을
   누적한다 (`core/FluentQuery.java`).
2. `and/or/not/distinct/fetchJoin/orderBy` 같은 빌더 메서드는 Specification
   합성이나 정렬/페치 조인 설정을 담당한다.
3. `fetch()/fetch(Sort)/fetch(Pageable)/fetchOne()/count()/exists()` 와 같은
   터미널 연산이 호출되면 `currentSpec()` 이 실행되어 최종 Specification 을
   조립한다.
   - 필요 시 CriteriaQuery 에 `distinct` 와 fetch join 을 적용한다.
   - 이후 `JpaSpecificationExecutor` 의 `findAll`, `findOne`, `count`,
     `exists` 를 호출해 실제 JPA CriteriaQuery → SQL 로 이어진다.

## 5. 전체 흐름 예시

```java
memberRepository.query()
    .where(Member::getStatus).equalTo("ACTIVE")
    .and(Member::getAge).greaterThanOrEqualTo(30)
    .fetch();
```

1. `where(Member::getStatus)` → Property 메서드 레퍼런스가 `status` 로 해석됨.
2. `.equalTo("ACTIVE")` → `Specification` `(root, query, cb)` 가 만들어짐.
3. `.and(Member::getAge)…` → 두 번째 Specification 생성 후 `and` 로 결합.
4. `.fetch()` → 누적된 Specification 을 `JpaSpecificationExecutor#findAll`
   에 전달하여 Criteria API → SQL 로 실행.

이 과정을 통해 Property 는 “필드 식별자 + 타입 정보”를 제공하고,
Specification 은 “조건 표현 + 조합” 역할을 담당하며, 최종적으로 Spring
Data JPA 가 CriteriaQuery 를 SQL 로 번역한다.
