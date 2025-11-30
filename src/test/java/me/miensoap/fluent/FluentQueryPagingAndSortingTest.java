package me.miensoap.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class FluentQueryPagingAndSortingTest extends
    AbstractFluentQueryIntegrationTest {

    @Test
    @DisplayName("status IN (ACTIVE,SUSPENDED) 페이징이 중복 없이 전체를 커버한다")
    void pagingAcrossMultiplePagesCoversAllResults() {
        Pageable first = PageRequest.of(0, 2, Sort.by("id"));
        Pageable second = first.next();
        Pageable third = second.next();

        Page<Member> firstPage = query().where(Member::getStatus).in(List.of("ACTIVE", "SUSPENDED")).fetch(first);
        Page<Member> secondPage = query().where(Member::getStatus).in(List.of("ACTIVE", "SUSPENDED")).fetch(second);
        Page<Member> thirdPage = query().where(Member::getStatus).in(List.of("ACTIVE", "SUSPENDED")).fetch(third);

        long total = memberRepository.count((root, q, cb) -> root.get("status").in(List.of("ACTIVE", "SUSPENDED")));
        assertThat(firstPage.getTotalElements()).isEqualTo(total);

        List<Long> combined = ids(firstPage.getContent());
        combined.addAll(ids(secondPage.getContent()));
        combined.addAll(ids(thirdPage.getContent()));
        assertThat(combined.stream().distinct().count()).isEqualTo(total);
    }

    @Test
    @DisplayName("fetch(Sort) 가 findAll(spec, sort) 와 동일한 정렬 순서를 유지한다")
    void fetchWithSortAlignsWithJpaRepository() {
        Sort sort = Sort.by(Sort.Order.desc("age"), Sort.Order.asc("id"));
        Specification<Member> active = (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");

        List<Member> expected = memberRepository.findAll(active, sort);
        List<Member> actual = query().where(active).fetch(sort);

        assertThat(ids(actual)).containsExactlyElementsOf(ids(expected));
    }

    @Test
    @DisplayName("orderBy DSL 로 누적한 정렬이 Sort 결과와 일치한다")
    void orderByDslMatchesManualSort() {
        Specification<Member> active = (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");
        List<Member> expected = memberRepository.findAll(active, Sort.by(Sort.Order.desc("age"), Sort.Order.asc("id")));

        List<Member> actual = query()
            .where(Member::getStatus).equalTo("ACTIVE")
            .orderBy(Member::getAge).descending()
            .orderBy(Member::getId).ascending()
            .fetch();

        assertThat(ids(actual)).containsExactlyElementsOf(ids(expected));
    }

    @Test
    @DisplayName("tags 조인 스펙에서 distinct() 가 중복 제거된 결과를 제공한다")
    void distinctEliminatesDuplicatesFromJoinBasedSpecifications() {
        Specification<Member> tagJoin = (root, query, cb) -> {
            root.join("tags");
            return cb.conjunction();
        };

        List<Member> duplicates = query().where(tagJoin).fetch();
        List<Member> deduped = query().where(tagJoin).distinct().fetch();

        List<Member> expected = memberRepository.findAll((root, query, cb) -> cb.isNotEmpty(root.get("tags")));

        assertThat(duplicates.size()).isGreaterThanOrEqualTo(deduped.size());
        assertThat(ids(deduped)).containsExactlyElementsOf(ids(expected));
    }
}
