# Spring Data JPA Fluent Query Builder

`me.miensoap.fluent` 모듈은 Spring Data JPA 의 `Specification` 기반 동적 쿼리를 가장 읽기 쉬운 Fluent DSL 로 구성할 수 있도록 도와주는 경량 라이브러리입니다. `spring-data-jpa` 외에는 어떠한 의존성도 요구하지 않으며, 기존 `JpaRepository`/`JpaSpecificationExecutor` 인터페이스 위에 자연스럽게 믹스인 되어 동작합니다.

## 주요 특징

- **타입 안전한 DSL**: `query().where(Member::getAge).greaterThanOrEqualTo(30)` 처럼 메서드 참조를 사용해 문자열 필드명 실수를 제거합니다. 연산 가능한 타입(숫자, 문자열, Boolean, 컬렉션 등)을 판단하여 잘못된 조합에는 친절한 메시지로 예외를 전달합니다.
- **명확한 연산 이름**: `gt/lt` 대신 `greaterThan`,`lessThanOrEqualTo` 같은 자명한 메서드를 제공하여 가독성을 높였습니다. 기존 이름은 하위 호환을 위해 Deprecated 상태로 유지됩니다.
- **Specification 혼합 지원**: 기존 `Specification<T>` 객체를 `where(specification)`, `and(specification)`, `or(specification)` 으로 주입하여 Fluent DSL 과 레거시 코드를 자연스럽게 섞을 수 있습니다.
- **정렬 DSL**: `orderBy(Member::getAge).descending().orderBy(Member::getId).ascending()` 과 같이 정렬 조건을 체인에 녹여 `fetch()` 호출 시 자동으로 `Sort` 를 구성합니다.
- **전역 옵션**: `distinct()` 로 중복 제거, `not()` 으로 전체 조건 부정 등 Specification 수준의 옵션을 간편하게 설정할 수 있습니다.
- **Fetch Join DSL**: `fetchJoin(Member::getTeam)` 혹은 `fetchJoin("team", JoinType.LEFT)` 로 N+1 문제가 발생하는 지연 로딩 연관을 즉시 로딩할 수 있습니다.
- **친절한 에러 메시지**: 필드 타입과 연산 이름을 포함한 검증 에러로 오용을 빠르게 발견할 수 있습니다.

## Gradle 설정

```groovy
dependencies {
    compileOnly 'org.springframework.boot:spring-boot-starter-data-jpa'
    compileOnly 'jakarta.persistence:jakarta.persistence-api'
}
```

테스트 코드에서 샘플 엔티티/리포지토리를 사용하고 싶다면 `org.projectlombok:lombok` 을 `testCompileOnly`/`testAnnotationProcessor` 로 추가합니다.

## 설치 및 사용법

1. 기존 리포지토리를 `FluentRepository` 를 상속하도록 수정합니다.

```java
public interface MemberRepository extends FluentRepository<Member, Long> {
    List<Member> findByStatusAndAgeGreaterThanEqual(String status, Integer age);
}
```

2. 서비스/애플리케이션 코드에서 `query()` 메서드를 통해 DSL 에 진입합니다.

```java
List<Member> members = memberRepository.query()
    .where(Member::getStatus).equalTo("ACTIVE")
    .and(Member::getAge).greaterThanOrEqualTo(30)
    .or(Member::getGrade).equalTo("VIP")
    .orderBy(Member::getAge).descending()
    .orderBy(Member::getId).ascending()
    .fetch();
```

3. 필요한 경우 JPA 의 `Specification` 과도 쉽게 조합합니다.

```java
Specification<Member> vipSpec = (root, query, cb) -> cb.equal(root.get("membershipType"), MembershipType.VIP);

List<Member> vipActives = memberRepository.query()
    .where(vipSpec)
    .and(Member::getStatus).equalTo("ACTIVE")
    .fetch();
```

4. 전역 옵션 예시:

```java
boolean exists = memberRepository.query()
    .where(Member::getStatus).equalTo("INACTIVE")
    .not() // not(status == INACTIVE)
    .distinct()
    .exists();
```

### Fetch Join 으로 N+1 제어

지연 로딩 연관을 즉시 로딩하고 싶다면 `fetchJoin()` 메서드를 사용하세요. 문자열 경로와 메서드 참조를 모두 지원하며, 중첩 경로(`"team.company"`)도 점 표기법으로 표현할 수 있습니다.

```java
List<Member> eagerLoaded = memberRepository.query()
    .where(Member::getStatus).equalTo("ACTIVE")
    .fetchJoin(Member::getTeam)
    .fetchJoin("tags", JoinType.LEFT)
    .distinct()
    .fetch();
```

카운트/exists 쿼리에서는 fetch join 이 자동으로 무시되므로 동일한 빌더를 재사용해도 안전합니다. 단, 컬렉션 fetch join 과 페이징을 동시에 사용하면 JPA 특성상 메모리 페이징이 발생하므로 주의하세요.

## 테스트 구조

`src/test/java/me/miensoap/fluent` 에는 H2 인메모리 DB 를 사용하는 간단한 Spring Boot 앱과 풍부한 통합 테스트가 포함되어 있습니다. 주요 테스트 클래스는 다음과 같이 기능별로 나뉘어 있습니다.

- `FluentQueryLogicalCombinationTest`: AND/OR 체인, Specification 믹스, not(), 빌더 독립성 검증
- `FluentQueryComparisonTest`: 숫자/날짜 경계값 비교(between, greaterThan 등)
- `FluentQueryStringOperationsTest`: 문자열 패턴(like, containing), 점 표기 경로, 타입 검증
- `FluentQueryCollectionOperationsTest`: in/notIn, isEmpty/isNotEmpty, Boolean 연산
- `FluentQueryTemporalOperationsTest`: LocalDateTime between/after/before
- `FluentQueryAggregateAndSingleResultTest`: fetch/fetchOne/count/exists 동작
- `FluentQueryPagingAndSortingTest`: 페이징, Sort DSL, distinct()

각 테스트는 Fluent DSL 결과와 Spring Data JPA 파생 메서드 또는 수동 Specification 결과를 id 기준으로 비교하여 동일성을 보장합니다.

## 개발 로드맵 (이미 구현됨)

`ir.md`, `plus.md`, `enhance.md` 문서에 정의된 요구사항은 모두 현재 코드에 반영되어 있습니다.

- 문자열 필드명 대신 메서드 참조 기반 DSL 제공
- 연산 메서드 명확화 (`greaterThan`, `equalTo` 등)
- 타입에 따라 허용/금지되는 연산을 검증
- Sort DSL 및 Specification 혼합, not/distinct 옵션 지원
- Empty collection, null 파라미터 등 에지 케이스 테스트 강화

## 샘플 프로젝트 실행

```bash
./gradlew test
```

테스트는 로컬 H2 메모리 DB 를 사용하므로 추가 환경 설정 없이 바로 실행됩니다. 출력되는 JUnit DisplayName 은 모두 한국어로 작성되어 있어 어떤 조건을 검증하는지 한눈에 파악할 수 있습니다.

## 라이선스

이 저장소의 라이선스 정책은 `LICENSE` 파일(없다면 기본 저작권 정책)에 따릅니다.
