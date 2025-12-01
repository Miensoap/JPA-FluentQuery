1. 문서 / 가이드 정비

 docs/property-chaining-plan.md 보완

CGLIB 기반 체인 해석 방식, 동작 방식(프록시 + getter 체인) 간단 요약 추가

제약 사항 명시: 인터페이스·final 클래스·기본 생성자 없는 타입은 지원하지 않음, 컬렉션/배열 인덱스 접근 불가 등

“람다는 항상 (RootType root) -> root.getX().getY() 형태”여야 한다는 사용 제약 명시

 docs/test-suite-reorg-plan.md 업데이트

패키지 구조 최종본 반영 (dsl / paging / fetch / property / performance)

신규 fixture(PostFixtures)와 repository(MemberLikePostRepository) 의 역할과 의존관계 간단 설명

2. Property Chaining 내부 구현 보완

 PropertyNameResolver / PropertyPathRecorder 결과 캐싱 도입

SerializedLambda signature 또는 implMethodName + implClass 조합을 키로 ResolvedProperty 캐싱

같은 프로퍼티 람다가 반복 호출될 때 CGLIB 프록시/리플렉션 비용 줄이기

 예외 메시지 개선

PropertyPathRecorder 에서 예외 발생 시, 가능한 경우

사용된 람다의 impl 메서드 이름

이미 쌓인 segments (user, post.user 등)
를 포함해 디버깅 용이성 높이기

 생성자 부작용 관련 방어/명시

CGLIB 프록시 생성 시 실제 생성자가 실행된다는 점을 문서화

필요하면 “생성자에서 외부 부작용을 갖지 않는 엔티티를 권장”하는 가이드 추가

3. exists() 동작 및 성능 점검

 exists() 의 count 기반 구현에 대한 벤치마크

fetch join 이 있는 경우 / 없는 경우 각각에 대해

데이터 규모별로(소/중/대) latency 확인

 필요 시 최적화 옵션 설계

옵션 1: exists() 호출 시 fetch join 을 제거한 사양으로 래핑 후, Spring Data 기본 exists 로 위임

옵션 2: 현행 count 기반을 유지하되, 설정 플래그나 고급 API로 “최적 exists 전략” 선택 가능하게 설계

 위 선택에 대한 결과/권장사항을 문서에 반영

4. Fetch Join / Specification 연동 검증 강화

 FluentQuery.currentSpec() 의 fetch join 삽입 조건 재점검

count 쿼리 여부 판단 로직이 모든 JPA 제공자/버전에서 안정적으로 동작하는지 확인

regression test에 “count + fetch join 혼합 시에도 예외 없음” 케이스 명시

 fetch join 테스트 추가/보완

string path vs property chaining path 가 동일한 쿼리 구조로 나오는지 비교 검증

nested collection join + aggregate (count/single result) 에서 중복 join 이 안 들어가는지 SQL 스냅샷으로 고정

5. 테스트 / CI 작업

 새 패키지 구조 기준으로 CI 파이프라인 검증

Gradle test task 가 변경된 패키지 경로를 모두 포함하는지, 스킵되는 스위트는 없는지 확인

 FluentQueryPropertyChainingTest 와 FluentQueryFetchJoinTest 간 fixture 의존성 검증

PostFixtures 사용이 양쪽에 제대로 공유되고 있는지

향후 다른 테스트에서 같은 그래프를 재사용할 때의 가이드(“이 fixture를 쓰라”)를 test 패키지 README나 주석에 추가

6. 사용자 API 관점의 DX 개선

 Property chaining 실패 시 사용자에게 보이는 메시지 정리

“final 클래스라서 안 된다”, “인터페이스라서 안 된다” 등을 구체적으로 안내

가능한 경우 대안 (엔티티 설계 변경, 단일 프로퍼티 레퍼런스 사용 등) 제안

 간단한 “사용 예제” 추가

docs 또는 dsl 패키지에

단일 프로퍼티: where(Chat::getArchivedAt).isNull()

체인: where(chat -> chat.getUser().getId()).eq(userId)

fetch join 과 함께 쓰는 예제

실제 코드에서 어떻게 쓰는지가 한눈에 보이도록 샘플 정리
