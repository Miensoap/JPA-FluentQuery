package me.miensoap.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class FluentQueryLogicalCombinationTest extends
    AbstractFluentQueryIntegrationTest {

    @Test
    @DisplayName("AND/OR 체인과 수동 Specification 결과가 일치한다")
    void andOrChainsMatchManualSpecifications() {
        Specification<Member> manual = (root, query, cb) -> cb.or(
            cb.equal(root.get("status"), "ACTIVE"),
            cb.equal(root.get("grade"), "VIP")
        );
        manual = manual.and((root, query, cb) -> cb.greaterThan(root.get("age"), 25));

        List<Member> manualResult = memberRepository.findAll(manual);

        List<Member> fluentResult = query()
            .where(Member::getStatus).equalTo("ACTIVE")
            .or(Member::getGrade).equalTo("VIP")
            .and(Member::getAge).greaterThan(25)
            .fetch();

        assertThat(ids(fluentResult)).containsExactlyElementsOf(ids(manualResult));
    }

    @Test
    @DisplayName("복잡한 체인에서도 AND/OR 연결 순서를 유지한다")
    void longChainsRespectConnectorOrder() {
        Specification<Member> manual = Specification.<Member>where((root, query, cb) -> cb.equal(root.get("status"), "ACTIVE"))
            .and((root, query, cb) -> cb.greaterThan(root.get("age"), 30))
            .or((root, query, cb) -> cb.equal(root.get("grade"), "PREMIUM"))
            .and((root, query, cb) -> cb.equal(root.get("role"), "PARTNER"));

        List<Member> manualResult = memberRepository.findAll(manual);

        List<Member> fluentResult = query()
            .where(Member::getStatus).equalTo("ACTIVE")
            .and(Member::getAge).greaterThan(30)
            .or(Member::getGrade).equalTo("PREMIUM")
            .and(Member::getRole).equalTo("PARTNER")
            .fetch();

        assertThat(ids(fluentResult)).containsExactlyElementsOf(ids(manualResult));
    }

    @Test
    @DisplayName("Specification과 Fluent DSL을 섞어도 동일한 결과를 낸다")
    void specificationMixingWorks() {
        Specification<Member> base = (root, query, cb) -> cb.equal(root.get("team").get("departmentCode"), "DEV");
        Specification<Member> extra = (root, query, cb) -> cb.equal(root.get("membershipType"), MembershipType.VIP);
        Specification<Member> alternate = (root, query, cb) -> cb.equal(root.get("status"), "SUSPENDED");

        List<Member> manualResult = memberRepository.findAll(base.and(extra).or(alternate));

        List<Member> fluentResult = query()
            .where(base)
            .and(extra)
            .or(alternate)
            .fetch();

        assertThat(ids(fluentResult)).containsExactlyElementsOf(ids(manualResult));
    }

    @Test
    @DisplayName("not() 호출 시 현재 명세 전체가 부정된다")
    void notOperatorInvertsCurrentSpecification() {
        List<Member> beforeNot = query()
            .where(Member::getStatus).equalTo("ACTIVE")
            .fetch();

        List<Member> afterNot = query()
            .where(Member::getStatus).equalTo("ACTIVE")
            .not()
            .fetch();

        assertThat(ids(beforeNot)).isNotEmpty();
        assertThat(ids(afterNot)).doesNotContainAnyElementsOf(ids(beforeNot));
    }

    @Test
    @DisplayName("각 query() 호출이 독립적인 빌더를 반환한다")
    void builderInstancesAreIsolated() {
        FluentQuery<Member> first = memberRepository.query();
        FluentQuery<Member> second = memberRepository.query();

        first.where(Member::getStatus).equalTo("ACTIVE");
        second.where(Member::getStatus).equalTo("INACTIVE");

        assertThat(first.fetch()).allMatch(member -> member.getStatus().equals("ACTIVE"));
        assertThat(second.fetch()).allMatch(member -> member.getStatus().equals("INACTIVE"));
        assertThat(first).isNotSameAs(second);
    }
}
