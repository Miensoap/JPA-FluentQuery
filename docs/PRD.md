이 문서는 **Spring Data JPA** 외에 \*\*추가 의존성 0(Zero Dependency)\*\*를 원칙으로 하며, 가장 직관적인 **빌더 패턴**을 채택했습니다.

-----

# 프로젝트: Spring Data JPA Fluent Query Builder

## 1\. 개요 (Overview)

  * **목적**: 문자열 파싱이나 복잡한 QueryDSL 설정 없이, 메서드 체이닝(Fluent API)만으로 동적 쿼리(`Specification`)를 생성하고 실행한다.
  * **핵심 원칙**: Spring Data JPA의 표준 스펙인 `Specification` 인터페이스를 내부적으로 조립하여, 타입 안전성과 가독성을 확보한다.
  * **Target Framework**: Spring Boot 2.x / 3.x (Spring Data JPA)

## 2\. 의존성 (Dependencies)

외부 라이브러리 없이 Spring Data JPA 표준 라이브러리만 사용한다.

**build.gradle (Module: library)**

```groovy
dependencies {
    // 컴파일 시점에만 필요 (사용자 프로젝트에는 이미 존재함)
    compileOnly 'org.springframework.boot:spring-boot-starter-data-jpa'
    compileOnly 'jakarta.persistence:jakarta.persistence-api' // or javax.persistence for legacy
}
```

## 3\. 아키텍처 및 클래스 설계 (Architecture)

### 3.1. 핵심 인터페이스 (`FluentRepository`)

사용자가 기존 `JpaRepository` 대신 상속받거나, 추가로 상속받을 인터페이스.

```java
public interface FluentRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    
    // 진입점 메서드
    default FluentQuery<T> query() {
        return new FluentQuery<>(this);
    }
}
```

### 3.2. 쿼리 빌더 (`FluentQuery`)

상태(State)를 관리하며 `Specification`을 누적하는 핵심 클래스.

  * **필드**:
      * `JpaSpecificationExecutor<T> executor`: 쿼리 실행기
      * `Specification<T> spec`: 현재까지 조립된 명세 (초기값: `Specification.where(null)`)
      * `boolean isOr`: 다음 조건 연결 시 OR 사용 여부 (기본값: `false` -\> AND)

### 3.3. 문법 흐름 (Syntax Flow)

`Subject(필드 선택)` -\> `Predicate(조건 정의)` -\> `Connector(AND/OR)` 순서로 순환.

## 4\. 상세 기능 명세 (Functional Requirements)

### 4.1. 필드 선택 단계 (`FieldSelector`)

빌더는 항상 **필드명을 입력받는 단계**에서 시작하거나 다시 돌아와야 한다.

  * **메서드**:
      * `FieldStep<T> where(String fieldName)`: 첫 조건 시작
      * `FieldStep<T> and(String fieldName)`: AND 조건으로 필드 선택
      * `FieldStep<T> or(String fieldName)`: OR 조건으로 필드 선택

### 4.2. 조건 정의 단계 (`FieldStep`)

필드가 선택된 상태에서 연산자(Operator)를 적용하고, 다시 빌더(`FluentQuery`)를 반환한다.
참고: `org.springframework.data.repository.query.parser.Part.Type`의 네이밍을 따른다.

| 메서드 시그니처 | 매핑되는 JPA Criteria Logic |
| :--- | :--- |
| `eq(Object val)` | `cb.equal(root.get(field), val)` |
| `ne(Object val)` | `cb.notEqual(root.get(field), val)` |
| `gt(Number val)` | `cb.gt(root.get(field), val)` |
| `ge(Number val)` | `cb.ge(root.get(field), val)` |
| `lt(Number val)` | `cb.lt(root.get(field), val)` |
| `le(Number val)` | `cb.le(root.get(field), val)` |
| `between(Comparable a, Comparable b)` | `cb.between(root.get(field), a, b)` |
| `after(Comparable val)` | `cb.greaterThan(root.get(field), val)` (날짜용) |
| `before(Comparable val)` | `cb.lessThan(root.get(field), val)` (날짜용) |
| `like(String pattern)` | `cb.like(root.get(field), pattern)` |
| `containing(String val)` | `cb.like(root.get(field), "%" + val + "%")` |
| `startingWith(String val)` | `cb.like(root.get(field), val + "%")` |
| `in(Collection<?> vals)` | `root.get(field).in(vals)` |
| `isNull()` | `cb.isNull(root.get(field))` |
| `isNotNull()` | `cb.isNotNull(root.get(field))` |
| `isTrue()` | `cb.isTrue(root.get(field))` |
| `isFalse()` | `cb.isFalse(root.get(field))` |

### 4.3. 결과 실행 단계 (Terminal Operations)

`FluentQuery` 클래스에 존재하며, 최종적으로 `executor`를 호출한다.

  * `List<T> fetch()`: `executor.findAll(spec)` 호출
  * `Page<T> fetch(Pageable pageable)`: `executor.findAll(spec, pageable)` 호출
  * `Optional<T> fetchOne()`: `executor.findOne(spec)` 호출
  * `long count()`: `executor.count(spec)` 호출
  * `boolean exists()`: `executor.exists(spec)` 호출

## 5\. 구현 가이드라인 (Implementation Guide)

### 5.1. `FluentQuery` 내부 로직

```java
public class FluentQuery<T> {
    private final JpaSpecificationExecutor<T> executor;
    private Specification<T> spec = Specification.where(null);

    public FluentQuery(JpaSpecificationExecutor<T> executor) {
        this.executor = executor;
    }

    // 조건 추가 로직 (핵심)
    protected void addCondition(Specification<T> newSpec, boolean isOr) {
        if (isOr) {
            this.spec = this.spec.or(newSpec);
        } else {
            this.spec = this.spec.and(newSpec);
        }
    }

    // AND 시작
    public FieldStep<T> and(String field) {
        return new FieldStep<>(this, field, false); // isOr = false
    }

    // OR 시작
    public FieldStep<T> or(String field) {
        return new FieldStep<>(this, field, true); // isOr = true
    }

    // ... fetch 메서드들 구현
}
```

### 5.2. `FieldStep` 내부 로직

`FieldStep`은 생성자에서 `builder`, `fieldName`, `isOr` 상태를 주입받아야 한다.
조건 메서드가 호출되면 `Specification`을 생성하여 `builder.addCondition()`에 넘기고, `builder`를 리턴하여 체이닝을 이어간다.

```java
public class FieldStep<T> {
    private final FluentQuery<T> builder;
    private final String field;
    private final boolean isOr;

    // 생성자 생략

    public FluentQuery<T> eq(Object val) {
        Specification<T> s = (root, query, cb) -> cb.equal(root.get(field), val);
        builder.addCondition(s, isOr);
        return builder;
    }
    
    // ... 나머지 연산자 구현
}
```

## 6\. 사용 예시 (Usage Scenario)

**Input (사용자 코드):**

```java
// Repository 정의
public interface MemberRepository extends FluentRepository<Member, Long> {}

// Service 사용
public void searchVips() {
    List<Member> vips = memberRepository.query()
        .where("status").eq("ACTIVE")
        .and("age").ge(20)
        .or("grade").eq("VIP")
        .and("lastLoginAt").after(LocalDateTime.now().minusDays(7))
        .fetch();
}
```

-----

### 🤖 AI 에이전트에게 전달할 프롬프트

> "위의 \*\*[Spring Data JPA Fluent Query Builder 기술 사양서]\*\*를 바탕으로 라이브러리를 구현해줘.
>
> 1.  **의존성을 최소화**해야 하므로 `spring-data-jpa` 외에는 아무것도 추가하지 마.
> 2.  `FluentRepository`, `FluentQuery`, `FieldStep` 세 클래스를 파일별로 나눠서 구현해.
> 3.  `Specification`을 생성할 때 람다 식을 사용하여 간결하게 작성해.
> 4.  사양서에 명시된 모든 연산자(eq, gt, like, containing 등)를 빠짐없이 구현해줘."

이 문서를 전달하면 AI가 `PartTree` 파싱 로직 없이도, Spring Data JPA의 강력한 기능을 그대로 활용하는 안전하고 가벼운 라이브러리를 작성할 것입니다.
