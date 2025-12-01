## 1. Property 해석 (단일 getter 람다)

### 1-1. 기본 getter 해석

* [ ] `Class::getXxx` 형태를 넘겼을 때 `path()` 가 `xxx`, `type()` 이 반환 타입인지
* [ ] `isXxx` 형태(boolean)도 `xxx` 로 잘 디캡italize 되는지
* [ ] 대문자로 시작하는 필드명 (예: `getURL`) 이 Introspector 규칙대로 기대한 이름으로 나오는지

### 1-2. 지원하지 않는 메서드에 대한 방어

* [ ] `Class::nonGetterMethod` 를 넘겼을 때 `IllegalArgumentException` 발생 여부 및 메시지 확인
* [ ] `null` property 를 넘겼을 때 NPE 대신 명시적인 예외 메시지 반환 여부

---

## 2. Property Chaining (람다 체인 해석)

### 2-1. 단일 체인 기본

* [ ] `root -> root.getUser()` 형태에서 `path = "user"`, `type = User.class`
* [ ] 2단계 체인 `root -> root.getUser().getId()` → `path = "user.id"`, `type = Long.class`
* [ ] 3단계 이상 체인 (예: `post.getMember().getTeam().getName()`) → `"member.team.name"` 과 leaf 타입 검증

### 2-2. getter-only 체인 제약

* [ ] 체인 중간에 non-getter 호출 시 (예: `getUser().toString()`) 예외 발생
* [ ] 체인이 전혀 없는 경우 (람다가 아무 것도 안 부르면) 예외 발생
* [ ] 체인 마지막에서만 primitive 반환, 중간은 reference 타입일 때 정상 동작 여부

### 2-3. 인터페이스/`final` 클래스/배열/컬렉션

* [ ] 체인 중간이 인터페이스 타입일 때 → “interface return types not supported” 예외
* [ ] 체인 중간이 `final` 클래스일 때 → “non-final types required” 예외
* [ ] 체인 중간이 배열 타입일 때 → 프록시 생성 안 하고 default 값 반환, 이후 체인에서 예외 발생 여부
* [ ] 체인 중간이 컬렉션 타입(List/Set 등)일 때 → 다음 getter 호출 시 적절한 예외 (지원하지 않음을 명확히)

### 2-4. primitive / wrapper / null 기본값

* [ ] leaf 타입이 primitive 일 때 `type()` 이 primitive class 인지
* [ ] primitive 타입의 defaultValue 가 모두 올바른지 (0, 0L, 0D, false, '\0' 등)
* [ ] reference 타입(엔티티, wrapper 등)은 항상 null 반환하고, 체인 중단 없이 계속 프록시가 붙는지

### 2-5. 람다 시그니처/SerializedLambda 처리

* [ ] 캡처 없는 lambda (`x -> x.getUser()`) 와 메서드 레퍼런스 (`Chat::getUser`) 모두 정상 처리
* [ ] root 타입을 잘못 지정한 람다 (예: 다른 타입의 getter) 에 대해 명확한 예외
* [ ] 같은 람다 인스턴스를 여러 번 resolve 할 때 결과 일관성

---

## 3. ResolvedProperty / Resolver 캐싱 및 동작 일관성

(캐싱 도입 시 전제)

* [ ] 같은 `Property` 에 대해 여러 번 resolve 했을 때 동일 인스턴스/동등성 보장 여부
* [ ] 캐시 키 기준(implClass + implMethodName + signature 등) 이 다를 경우 다른 결과를 잘 구분하는지
* [ ] 캐시가 있어도 예외가 발생해야 하는 잘못된 람다에 대해서는 캐시가 오염되지 않는지

---

## 4. FluentQuery + Specification 연동 (FieldStep)

### 4-1. 단일 필드 조건

* [ ] `eq(Chat::getArchivedAt, null)` → `archivedAt IS NULL`
* [ ] `eq(Chat::getTitle, "foo")` → `title = 'foo'`
* [ ] `gt(Chat::getUpdatedAt, now)` → `updatedAt > :now`
* [ ] `like(Chat::getTitle, "%foo%")` 등 문자열 조건이 JPA Criteria 와 일치하는지

### 4-2. 체인 필드 조건

* [ ] `where(chat -> chat.getUser().getId()).eq(userId)` → join + `user.id = :userId` 혹은 path(`"user.id"`)로 처리되는지
* [ ] `where(post -> post.getMember().getTeam().getName()).eq("teamA")` 와 string 기반 `"member.team.name"` 이 동일 쿼리 생성하는지
* [ ] 체인 경로와 string 경로를 혼합 사용했을 때 충돌 없이 동작하는지

### 4-3. 논리 연산 / 복합 조건

* [ ] `.and()`, `.or()` 조합 시 CriteriaPredicate 와 일치하는지
* [ ] `where(userId).and(archivedAtIsNull).and(folderIsNull)` 의 순서와 상관없이 동등한 결과
* [ ] 빈 조건(`where()` 만 쓰고 조건 없음)일 때 전체 조회가 되는지

---

## 5. Fetch Join + FluentQuery 연동

### 5-1. 기본 fetch join

* [ ] `fetchJoin(Chat::getUser)` → `join chat.user fetch` 가 들어가는지
* [ ] 문자열 path 기반 `fetchJoin("user")` 와 property 기반 `fetchJoin(Chat::getUser)` 비교
* [ ] 컬렉션 연관 `fetchJoin(Chat::getMessages)` → left join fetch messages

### 5-2. nested fetch join / join 타입

* [ ] `fetchJoin(Post::getMember)` + `fetchJoin("member.team")` 조합에서 중복 join 없이 동작하는지
* [ ] join 타입 override (LEFT vs INNER) 시 SQL 에 올바른 join type 반영되는지
* [ ] 한 경로에 대해 여러 번 fetchJoin 호출해도 중복 fetch 가 생성되지 않는지

### 5-3. fetch join + paging / aggregate

* [ ] fetch join 이 있는 상태에서 paging (Page, Slice) 이 잘 동작하는지
* [ ] count 쿼리에서 fetch join 이 제거되는지 (혹은 생성되지 않는지)
* [ ] groupBy / aggregate (`count`, `sum`) 와 fetch join 조합에서 JPA 표준에 어긋나는 SQL 이 생성되지 않는지

---

## 6. exists() 동작

### 6-1. 기본 동작

* [ ] 조건 없이 exists() → 테이블에 아무 row 있어도 true
* [ ] where 조건을 준 exists() → 해당 조건에 맞는 row 가 있으면 true/false

### 6-2. fetch join 이 있는 경우

* [ ] fetch join 추가 후 exists() → Hibernate 에러 없이 count 기반 exists 실행되는지
* [ ] fetch join 이 없는 경우와 SQL 형태/실행 결과 비교

### 6-3. 성능/최적화 관련

* [ ] 대량 데이터에서 exists() 호출이 정상 종료되는지 (타임아웃 등 없는지)
* [ ] 동일 조건에 대해 Spring Data 기본 exists (SELECT 1) 과 결과 일관성 검증

---

## 7. Repository 레벨 연동 (MemberLikePostRepository 등)

### 7-1. 기본 조회 메서드

* [ ] 체인 기반 DSL 을 사용하는 repository 메서드가 제대로 동작하는지 (예: userId + archivedAt + folder 조건 등)
* [ ] 같은 조건을 기존 JpaRepository 네이밍 쿼리 메서드로 구현했을 때와 결과가 동일한지

### 7-2. 복합 도메인 시나리오

* [ ] Post–Like–Member 그래프에서

  * memberId 로 like 를 조회
  * post 조건 + member 조건 + like 조건 조합
    을 체인 기반 DSL 로 표현했을 때 기대한 레코드만 조회되는지
* [ ] fetch join + property chaining 을 섞어서 사용했을 때 N+1 이 제거되는지 (SQL 개수 검사)

---

## 8. 에러/에지 케이스

### 8-1. 잘못된 property 참조

* [ ] 삭제되었거나 이름이 바뀐 getter 를 참조하는 람다 → 테스트 컴파일 실패로 검출되는지 (이건 의도 검증용)
* [ ] 런타임에만 드러나는 시나리오(리플렉션 실패 등)에 대해 의미 있는 메시지 나오는지

### 8-2. 도메인 모델 제약

* [ ] final 클래스/인터페이스/추상 클래스/프록시가 복잡한 엔티티 구조에 대해

  * 어떤 체인이 허용되고
  * 어떤 체인이 정확히 어떤 메시지로 막히는지
    를 커버하는 통합 테스트
* [ ] 생성자에서 부작용이 있는 엔티티에 대해 체인 해석 시 부작용이 실제로 발생하는지 여부를 확인하는 “문서용 테스트”
  (실제 사용에선 지양해야 함을 보여주는 가드용)

---

## 9. 테스트 인프라/리그레션

### 9-1. 패키지/구조 리그레션

* [ ] dsl / paging / fetch / property / performance 각각에서 공용 fixture 가 정상 공유되는지
* [ ] 패키지 구조 변경 후, 이전 버전에서 커버하던 시나리오가 새 구조에서도 모두 커버되고 있는지 (누락된 테스트 없음)

### 9-2. 퍼포먼스/회귀

* [ ] 체인 길이에 따른 성능(1단계, 2단계, 3단계) 비교 테스트 (대략적인 smoke 수준)
* [ ] JPA provider/Hibernate 버전 업 시에도 fetch join + count + exists 조합이 깨지지 않는지 확인하는 회귀 테스트

---

위 시나리오들을 기준으로,

* `unit (resolver/recorder)`,
* `integration (Specification + JPA)`,
* `repository-level (실제 도메인 쿼리)`
