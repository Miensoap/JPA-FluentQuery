# Spring Data JPA Fluent Query

### JpaRepository Query Method 와 QueryDsl 그 사이 어딘가

>아주 간단 -> JpaRepository 사용   
>더 복잡한 요구사항 -> querydsl 사용

그 사이에서 jpql이나 장황한 이름의 메서드를 사용하던 코드들을 **가독성**과 **안정성** 모두 챙기며 대체할 수 있는 방법을 고안했습니다.


### 컨셉

1. 기존 `JpaRepository` 의 네이밍 기반 쿼리 생성 방식의 단점인 메서드명이 장황해지는 문제를 fluent 스타일로 해결
2. 문자열 기반 쿼리의 컴파일 시 점에 오류를 발견하기 힘들다는 단점을 kotlin jdsl 에서 아이디어를 얻어 getter 를 통한 필드 resolving 으로 보완
3. 최대한 로직을 줄임. criteria 쿼리 생성을 직접 제어하지 않고, spring data jpa common의 `Specification`을 그대로 사용


### 로드맵

1. "그 사이" 지점에서 자주 사용되는 패턴인 fetch join을 지원
2. ...

