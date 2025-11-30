1. 테스트만 추가하면 되는 것들 (기능은 이미 구현됨)

이미 메서드는 있는데, 케이스를 고정해 두면 좋은 부분들입니다.

AND / OR 조합 검증

where("status").eq("ACTIVE").or("grade").eq("VIP")
where-AND-or-AND 같은 긴 체인에서
Specification 수동 작성과 결과가 완전히 일치하는지 테스트.

현재 isOr 플래그로 spec.or(...) / spec.and(...) 를 잘 타고 있으니, 동작 정의를 테스트로 못 박는 수준입니다.

비교/범위 연산 경계값

gt / ge / lt / le / between / after / before 가
“포함/미포함”을 어떻게 처리하는지 경계값(=, ±1, 같은 시간값)으로 검증.

예: age = 30인 엔티티를 두고 gt(30) 과 ge(30) 의 차이,
between(25, 30) 이 30을 포함하는지 여부 등을 테스트로 명확화.

null / 빈 컬렉션 인자 처리

in(List.of()), notIn(List.of()) 에서 쿼리가 어떻게 동작하는지(항상 false? 그대로 in()?)를 테스트해 두기.

like(null), containing(null) 같은 경우는 NPE가 날지, IllegalArgument 예외를 던질지 등
현재 구현 상태에 맞춰 기대 동작을 테스트로 고정.

boolean / 컬렉션 연산

isTrue / isFalse 가 실제 boolean 필드에 정확히 매핑되는지.

isEmpty / isNotEmpty 가 JPA Criteria의 isEmpty/isNotEmpty 와 같은 결과를 내는지
(엔티티에 컬렉션 필드 추가해서 통합 테스트).

결과 없음 / 다건 결과 케이스

fetch() 가 빈 리스트, fetchOne() 이 Optional.empty() 를 반환하는 상황에 대한 테스트.

스펙 상 fetchOne() 이 “정확히 한 건(single result)”이 아니라
단순히 findOne(spec) 위임이라면, 다건일 때 예외가 터지는지도 테스트로 확인.

페이징 세부 검증

현재는 Pageable.ofSize(2) 한 페이지만 확인하고 있으므로,
2페이지, 3페이지를 연속 조회했을 때 중복/누락 없이 전체를 커버하는지 확인하는 테스트.

Builder 재사용/독립성

memberRepository.query() 를 여러 번 호출했을 때 서로 다른 FluentQuery 인스턴스가 생기는지,
한 번 쓴 builder를 다시 써도 이전 조건이 누적/오염되지 않는지에 대한 테스트.

2. 기능 추가 + 테스트가 같이 필요한 것들

여기부터는 실제로 메서드를 더 늘려야 하는 영역입니다.

2-1. 정렬(Sort) 지원

현재 fetch(Pageable) 로 페이징+정렬은 같이 처리할 수 있지만,
단순 fetch() 에는 정렬 개념이 없습니다.

후보:

orderBy(String field).asc() / desc() 형태의 DSL을 추가하고,
내부에 Sort 를 들고 있다가 fetch() 시 findAll(spec, sort) 호출.

혹은 fetch(Sort sort) 메서드를 추가해서,
memberRepository.query().where("age").ge(30).fetch(Sort.by("age").descending()) 사용.

추가 후에는 “파생 메서드 + Sort를 쓴 결과”와 완전히 동일한지 테스트 필요.

2-2. Specification 직접 주입/조합 API

지금은 오로지 where/and/or("field") 로만 조건을 시작할 수 있습니다.

확장 아이디어:

public FluentQuery<T> where(Specification<T> baseSpec)
public FluentQuery<T> and(Specification<T> extra)
public FluentQuery<T> or(Specification<T> extra)

이렇게 해두면 기존에 존재하던 Specification 과 Fluent DSL을 섞어서 쓸 수 있습니다.

이 경우에는 “수동 작성한 spec + DSL spec” 이 조합된 결과가 기대대로 나오는지 테스트를 추가해야 합니다.

2-3. 전역 not, distinct 등 쿼리 레벨 옵션

not()

전체 조건을 부정하는 not() 같은 기능이 있으면 유용할 수 있습니다.
예: .where("status").eq("ACTIVE").not().fetch();

구현은 내부 spec 을 감싸는 Specification.not(spec) 형태가 될 것이고,
기대 결과를 수동 spec과 비교하는 테스트 필요.

distinct()

조인/컬렉션 쿼리 사용 시 결과 중복을 제거하기 위한 distinct() 플래그 지원.

JPA Specification 에서는 (root, query, cb) -> { query.distinct(true); return spec.toPredicate(...); } 패턴이 필요하므로,
이런 래핑 로직 추가 + 중복 행을 일부러 만들어 놓고 distinct 여부 테스트.

2-4. 문자열 연산 확장 (IgnoreCase 등)

Spring Data 파생 메서드가 갖고 있는 것과 맞출 의향이 있다면:

likeIgnoreCase, containingIgnoreCase, startingWithIgnoreCase, endingWithIgnoreCase 등.

구현은 cb.lower(root.get(field)) + toLowerCase() 조합으로 가능하고,
같은 조건을 Specification 으로 수동 작성해 비교하는 테스트를 넣으면 됩니다.

2-5. 중첩 경로/연관관계 필드 지원

현재는 root.get(field) 만 쓰고 있어서 단일 속성에만 대응합니다.

스펙 확장:

"team.name", "address.city" 같은 dotted path를 받아서
root.get("team").get("name") 로 풀어내도록 구현.

연관 엔티티/embedded 타입을 가진 엔티티를 하나 추가하고,

where("team.name").eq("DEV") 가 join + 조건으로 제대로 동작하는지 테스트가 필요합니다.

2-6. 에러/검증용 기능

잘못된 필드명에 대해:

현재는 JPA 메타모델 검증 없이 문자열만 사용하므로 런타임에만 실패합니다.

선택:

그냥 지금처럼 root.get(field) 에서 예외가 나도록 두고,
이를 기대하는 테스트를 추가.

혹은 FieldStep 생성 시 메타모델을 이용한 필드명 검증 기능을 추가하고
“존재하지 않는 필드 이름이면 명시적인 IllegalArgumentException”을 던지는 쪽으로 설계 변경.

이 경우는 “기능 + 테스트”를 함께 손봐야 합니다.
