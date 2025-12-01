package me.miensoap.fluent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.miensoap.fluent.entity.Member;

class FluentQueryFetchJoinTest extends AbstractFluentQueryIntegrationTest {

    @Test
    @DisplayName("fetchJoin(Member::getTeam) 으로 lazy 연관이 즉시 로딩된다")
    void fetchJoinLoadsLazyAssociation() {
        entityManager.flush();
        entityManager.clear();
        clearExecutedSql();

        List<Member> members = query()
            .fetchJoin(Member::getTeam)
            .fetch();

        List<String> sql = executedSql();
        assertThat(sql)
            .anyMatch(statement -> statement.toLowerCase().contains(" join ") && statement.toLowerCase().contains(" team "));

        clearExecutedSql();

        assertThatCode(() -> members.forEach(member -> member.getTeam().getName()))
            .doesNotThrowAnyException();
        assertThat(members)
            .allMatch(member -> Hibernate.isInitialized(member.getTeam()));
        assertThat(executedSql()).isEmpty();
    }

    @Test
    @DisplayName("fetchJoin() 은 count/exists 와 함께 사용해도 안전하다")
    void fetchJoinWithAggregateOperations() {
        long repositoryCount = memberRepository.count();
        clearExecutedSql();

        long counted = query()
            .fetchJoin(Member::getTeam)
            .count();

        List<String> sql = executedSql();
        assertThat(sql)
            .hasSize(1)
            .allMatch(statement -> !statement.toLowerCase().contains(" join "));
        assertThat(counted).isEqualTo(repositoryCount);
    }

    @Test
    @DisplayName("컬렉션 fetch join 결과도 distinct() 로 기준 엔티티 수를 유지한다")
    void collectionFetchJoinWorksWithDistinct() {
        entityManager.flush();
        entityManager.clear();
        clearExecutedSql();

        List<Member> deduped = query()
            .fetchJoin(Member::getTags)
            .distinct()
            .fetch();

        List<String> sql = executedSql();
        assertThat(sql)
            .anyMatch(statement -> statement.toLowerCase().contains(" join ") && statement.toLowerCase().contains("member_tags"));
        clearExecutedSql();

        assertThat(deduped).hasSize((int) memberRepository.count());
        assertThat(ids(deduped)).isEqualTo(ids(memberRepository.findAll()));
    }
}
