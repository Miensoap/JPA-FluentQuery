package me.miensoap.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ensures that derived Spring Data queries keep parity with the fluent DSL.
 */
class MemberRepositoryFluentParityTest extends AbstractFluentQueryIntegrationTest {

    @Test
    @DisplayName("status+age 조건 파생 쿼리와 fluent 조합이 동일하다")
    void statusAndAgeQueriesMatch() {
        List<Member> derived = memberRepository.findByStatusAndAgeGreaterThanEqual("ACTIVE", 30);
        List<Member> fluent = query()
            .where(Member::getStatus).equalTo("ACTIVE")
            .and(Member::getAge).greaterThanOrEqualTo(30)
            .fetch();

        assertThat(ids(fluent)).containsExactlyElementsOf(ids(derived));
    }

    @Test
    @DisplayName("grade.endsWith 파생 쿼리와 fluent endingWith 가 일치한다")
    void gradeEndingWithMatches() {
        List<Member> derived = memberRepository.findByGradeEndingWith("IC");
        List<Member> fluent = query().where(Member::getGrade).endingWith("IC").fetch();

        assertThat(ids(fluent)).containsExactlyElementsOf(ids(derived));
    }

    @Test
    @DisplayName("status.notContaining 파생 쿼리가 fluent notContaining 과 동일하다")
    void statusNotContainingMatches() {
        List<Member> derived = memberRepository.findByStatusNotContaining("ACT");
        List<Member> fluent = query().where(Member::getStatus).notContaining("ACT").fetch();

        assertThat(ids(fluent)).containsExactlyElementsOf(ids(derived));
    }

    @Test
    @DisplayName("role.notIn 파생 쿼리와 fluent notIn 결과가 같다")
    void roleNotInMatches() {
        List<Member> derived = memberRepository.findByRoleNotIn(List.of("USER", "ANALYST", "STAFF"));
        List<Member> fluent = query().where(Member::getRole).notIn(List.of("USER", "ANALYST", "STAFF")).fetch();

        assertThat(ids(fluent)).containsExactlyElementsOf(ids(derived));
    }

    @Test
    @DisplayName("tags.isEmpty 파생 쿼리와 fluent isEmpty 가 일치한다")
    void tagsIsEmptyMatches() {
        List<Member> derived = memberRepository.findByTagsIsEmpty();
        List<Member> fluent = query().where(Member::getTags).isEmpty().fetch();

        assertThat(ids(fluent)).containsExactlyElementsOf(ids(derived));
    }

    @Test
    @DisplayName("findFirstByRoleOrderByIdAsc 와 fluent order+fetchOne 결과가 동일하다")
    void findFirstMatchesOrderedFetchOne() {
        Optional<Member> derived = memberRepository.findFirstByRoleOrderByIdAsc("USER");
        Optional<Member> fluent = query()
            .where(Member::getRole).equalTo("USER")
            .orderBy(Member::getId).ascending()
            .fetchOne();

        assertThat(derived.map(Member::getId)).isEqualTo(fluent.map(Member::getId));
    }
}
