package me.miensoap.fluent.tests.integration.core;

import me.miensoap.fluent.support.AbstractFluentQueryIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.miensoap.fluent.support.entity.Member;

class FluentQuerySpecificationIntegrationTest extends AbstractFluentQueryIntegrationTest {

    @Test
    @DisplayName("문자열 경로와 프로퍼티 참조를 조합한 where/and 체인이 동일한 Predicate 를 생성한다")
    void combinesStringAndPropertyPaths() {
        List<Member> fluent = memberRepository.query()
            .where(Member::getStatus).equalTo("ACTIVE")
            .and("team.departmentCode").equalTo("DEV")
            .and(member -> member.getAddress().getCountry()).equalTo("KR")
            .fetch();

        assertThat(fluent)
            .allSatisfy(member -> {
                assertThat(member.getStatus()).isEqualTo("ACTIVE");
                assertThat(member.getTeam().getDepartmentCode()).isEqualTo("DEV");
                assertThat(member.getAddress().getCountry()).isEqualTo("KR");
            });
    }

    @Test
    @DisplayName("not() 과 distinct() 제어가 CriteriaQuery 에 정확히 반영된다")
    void appliesNotAndDistinctFlags() {
        LocalDateTime bound = now.minusDays(4);

        List<Member> distinct = memberRepository.query()
            .where(Member::getLastLoginAt).before(bound)
            .not()
            .distinct()
            .fetch();

        assertThat(distinct)
            .allMatch(member -> !member.getLastLoginAt().isBefore(bound));
    }

    @Test
    @DisplayName("count/exists 는 동일한 Specification 으로 일관된 결과를 반환한다")
    void countAndExistsRemainConsistent() {
        Member reference = memberRepository.findAll().get(0);

        long count = memberRepository.query()
            .where(Member::getMembershipType).equalTo(reference.getMembershipType())
            .count();

        boolean exists = memberRepository.query()
            .where(Member::getMembershipType).equalTo(reference.getMembershipType())
            .exists();

        assertThat(count).isGreaterThan(0);
        assertThat(exists).isTrue();
    }
}
