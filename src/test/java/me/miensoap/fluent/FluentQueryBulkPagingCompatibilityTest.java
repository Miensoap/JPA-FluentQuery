package me.miensoap.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Verifies that fetch(Pageable) integrates seamlessly with Spring Data Page APIs.
 */
class FluentQueryBulkPagingCompatibilityTest extends AbstractFluentQueryIntegrationTest {

    @BeforeEach
    void addBulkMembers() {
        List<Member> bulkMembers = IntStream.range(0, 60)
            .mapToObj(i -> new Member(
                "ACTIVE",
                18 + (i % 40),
                "BULK_%02d".formatted(i),
                i % 2 == 0 ? "ANALYST" : "USER",
                now.minusHours(i),
                "bulk" + i + "@corp.com",
                List.of("bulk" + (i % 5)),
                Boolean.TRUE,
                MembershipType.BASIC,
                i % 2 == 0 ? devTeam : opsTeam,
                new Address("BulkCity" + (i % 4), "KR")
            ))
            .collect(Collectors.toList());

        memberRepository.saveAll(bulkMembers);
    }

    @Test
    @DisplayName("fetch(Pageable) 가 findAll(spec, pageable) 과 동일하게 동작한다")
    void pageableResultsMatchJpaSpecificationExecutor() {
        Specification<Member> active = (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("age"), Sort.Order.asc("id")));

        Page<Member> expected = memberRepository.findAll(active, pageable);
        Page<Member> actual = query().where(Member::getStatus).equalTo("ACTIVE").fetch(pageable);

        assertThat(actual.getTotalElements()).isEqualTo(expected.getTotalElements());
        assertThat(actual.getTotalPages()).isEqualTo(expected.getTotalPages());
        assertThat(actual.getNumber()).isEqualTo(pageable.getPageNumber());
        assertThat(actual.getContent().stream().map(Member::getId).toList())
            .containsExactlyElementsOf(expected.getContent().stream().map(Member::getId).toList());
    }

    @Test
    @DisplayName("Page 인터페이스 네비게이션과 DSL orderBy 조합이 정상 동작한다")
    void pageableNavigationAndDslOrderingAreRespected() {
        Specification<Member> active = (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");
        Pageable pageable = PageRequest.of(1, 15, Sort.by(Sort.Order.asc("grade")));

        Page<Member> expected = memberRepository.findAll(active, PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort().and(Sort.by(Sort.Order.desc("age")))
        ));

        Page<Member> actual = query()
            .where(Member::getStatus).equalTo("ACTIVE")
            .orderBy(Member::getAge).descending()
            .fetch(pageable);

        assertThat(actual.hasPrevious()).isTrue();
        assertThat(actual.hasNext()).isTrue();
        assertThat(actual.getSort()).isEqualTo(expected.getSort());
        assertThat(actual.getContent().stream().map(Member::getId).toList())
            .containsExactlyElementsOf(expected.getContent().stream().map(Member::getId).toList());
    }

    @Test
    @DisplayName("JpaRepository.findAll(pageable) 과 query().fetch(pageable) 결과가 완전히 동일하다")
    void repositoryFindAllAndFluentFetchReturnIdenticalPages() {
        Pageable pageable = PageRequest.of(2, 10, Sort.by(Sort.Order.asc("grade"), Sort.Order.desc("id")));

        Page<Member> repositoryPage = memberRepository.findAll(pageable);
        Page<Member> fluentPage = query().fetch(pageable);

        assertThat(fluentPage.getNumber()).isEqualTo(repositoryPage.getNumber());
        assertThat(fluentPage.getTotalElements()).isEqualTo(repositoryPage.getTotalElements());
        assertThat(fluentPage.getTotalPages()).isEqualTo(repositoryPage.getTotalPages());
        assertThat(fluentPage.getContent().stream().map(Member::getId).toList())
            .containsExactlyElementsOf(repositoryPage.getContent().stream().map(Member::getId).toList());
    }
}
