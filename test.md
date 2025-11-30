1. 테스트 클래스는 어떻게 나누면 좋은지
1-1. 플루언트 DSL 기능 축으로 쪼개기 (추천)

비교/범위 연산 전용

클래스 예: FluentQueryComparisonTest

다루는 것:

eq / ne / gt / ge / lt / le / between / after / before

숫자/시간 경계값(=, ±1, 같은 시간값) 테스트

목적:

CriteriaBuilder의 inclusive/exclusive 논리가 DSL과 정확히 일치하는지 고정.

문자열 연산 전용

클래스 예: FluentQueryStringOperationsTest

다루는 것:

like / notLike / containing / notContaining / startingWith / endingWith

대소문자, 공백, 특수문자(예: %, _) 포함 케이스

결과 없음 / 모두 매칭 / 일부 매칭 케이스

목적:

Spring Data 파생 메서드(findByXxxContaining 등)와의 동등성 검증을 집중.

컬렉션/ElementCollection 전용

클래스 예: FluentQueryCollectionOperationsTest

다루는 것:

in / notIn / isEmpty / isNotEmpty

tags 컬렉션의 크기 0 / 1 / N, 중복 값, 특정 값 포함 여부

in(List.of()), notIn(List.of()) 같은 빈 컬렉션 인자

목적:

IN 절, 컬렉션 empty/not empty 조건이 Expected와 정확히 일치하는지.

시간/날짜 전용

클래스 예: FluentQueryTemporalOperationsTest

다루는 것:

between, after, before + LocalDateTime 의 경계값

now 기준 ±n일, ±n시간 데이터 분포를 세밀하게 배치

목적:

시간 비교 시 inclusive/exclusive, millis 이하 차이 등 오해 가능 지점을 테스트로 고정.

집계/단일 조회/존재 여부 전용

클래스 예: FluentQueryAggregateAndSingleResultTest

다루는 것:

fetchOne / count / exists

결과 0건 / 1건 / N건(특히 fetchOne 시 예외 동작)

목적:

“한 건만 기대하는” 쿼리의 결과 정의 및 에러 상황을 명시적으로 보장.

페이징/정렬 전용

클래스 예: FluentQueryPagingAndSortingTest

다루는 것:

fetch(Pageable) (Page 여러 페이지 연속 조회)

정렬 지원을 추가하면, Sort와의 조합

목적:

전체 카운트, 페이지별 요소 수, 중복/누락 없이 전체 커버되는지 확인.

논리 조합(AND/OR)/조합 스펙 전용

클래스 예: FluentQueryLogicalCombinationTest

다루는 것:

where + and + or 조합, “status = ACTIVE AND (age > 30 OR grade = 'VIP')” 같은 패턴

동일 조건을 수동 Specification 으로 구성해 비교

목적:

내부 spec.and(...) / spec.or(...) 체인이 의도대로 작동하는지 확인.

공통 데이터 셋업(@BeforeEach)은 추상 베이스 클래스(AbstractFluentQueryIntegrationTest)로 올리고,
각 테스트 클래스에서 상속받는 구조로 만들면 중복을 줄일 수 있습니다.

2. 어떤 엔티티 구성이 엣지 케이스를 커버하기 좋은지

현재 Member 엔티티는 이미 다음을 잘 커버하고 있습니다.

숫자: age

문자열: status, grade, role, email

시간: lastLoginAt

컬렉션: @ElementCollection List<String> tags

여기에 테스트 커버리지를 극대화하려면, 다음 필드/엔티티를 추가하는 것을 추천드립니다.

2-1. Member 엔티티 필드 확장 아이디어

Boolean 필드

예: private Boolean active;

테스트 대상:

isTrue(), isFalse()

null 포함 3가지 상태(true/null/false) 분기

이유:

boolean 연산이 실제 Criteria isTrue/isFalse 와 맞게 매핑되는지 검증 필요.

Enum 필드

예: private MembershipType membershipType; (BASIC / PREMIUM / VIP 등)

테스트 대상:

eq, in, notIn를 enum에 적용했을 때 동작

이유:

enum은 DB에 문자열/ORDINAL 등으로 저장되므로, 타입 캐스팅/매핑에서 오류가 나기 쉬움.

null 허용 필드 시나리오 분명히 하기

현재도 email 등은 null 가능하지만, 테스트 데이터에 “null / 빈 문자열 / 정상 값”을 의도적으로 배치:

email == null

email == "" (빈 문자열)

email == " " (공백)

email == "user@corp.com
"

테스트 대상:

isNull / isNotNull

like / containing 등에서 null/빈 문자열 처리.

숫자 경계값용 필드

지금 age 하나로도 가능하지만, 좀 더 다양한 타입을 보려면:

Long points;, Double score; 같은 필드 추가

테스트 대상:

gt/ge/lt/le/between 이 Number 타입 전반에서 문제 없는지.

2-2. 연관 관계 / 중첩 프로퍼티용 엔티티 추가

플루언트 DSL을 "team.name" 처럼 dot-path로 확장할 계획이 있다면,
관계/임베디드 엔티티를 반드시 하나 이상 추가해 두는 것이 좋습니다.

연관관계 예시: Team

Member 에:

@ManyToOne private Team team;

Team 엔티티:

id, name, departmentCode 정도의 간단한 필드

테스트 대상:

where("team.name").eq("...")

where("team.departmentCode").in(List.of(...))

이유:

join이 실제로 발생하고, 경로 탐색이 제대로 root.get("team").get("name") 로 매핑되는지 확인 가능.

임베디드 값 타입 예시: Address

@Embeddable Address { String city; String country; }

Member 에:

@Embedded private Address address;

테스트 대상:

where("address.city").eq("Seoul")

null 주소, 일부만 채워진 주소 등.

이유:

embedded 타입의 경로 처리도 컬럼 네이밍/매핑에 따라 오류가 나기 쉬움.

2-3. 데이터 세트 구성 팁

각 테스트 모듈에서 공통으로 쓰기 좋은 “패턴 있는 데이터”를 미리 정해 두면 좋습니다.

예를 들어:

status: "ACTIVE", "INACTIVE", "SUSPENDED", "DELETED"

age: null, 20, 25, 30, 35, 40

grade: "BASIC", "PREMIUM", "VIP", "VIP_GOLD" (문자열 패턴/부분 일치용)

role: "USER", "ADMIN", "STAFF", "PARTNER"

email:

null

"alice@corp.com"

"bob@corp.com"

"sue@partner.com"

tags:

[]

["vip"]

["vip", "legacy"]

["partner", "beta"]

이렇게 “의도적으로 겹치고 엣지에 걸리는 값들”을 사용하면,
각 테스트 클래스가 동일한 setUp() 을 공유하면서도 다양한 연산을 안정적으로 검증할 수 있습니다.
