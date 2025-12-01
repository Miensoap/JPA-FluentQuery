package me.miensoap.fluent.tests.integration.performance;

import me.miensoap.fluent.support.AbstractFluentQueryIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import me.miensoap.fluent.support.entity.Member;

class FluentQueryAggregateAndSingleResultTest extends AbstractFluentQueryIntegrationTest {

    @Test
    @DisplayName("status=UNKNOWN 으로 fetch() 하면 빈 리스트가 반환된다")
    void fetchReturnsEmptyListWhenNoMatch() {
        assertThat(query().where(Member::getStatus).equalTo("UNKNOWN").fetch()).isEmpty();
    }

    @Test
    @DisplayName("email=missing@corp.com fetchOne() 은 Optional.empty() 를 반환한다")
    void fetchOneReturnsEmptyOptionalWhenNoResult() {
        Optional<Member> result = query().where(Member::getEmail).equalTo("missing@corp.com").fetchOne();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("status=ACTIVE fetchOne() 은 다건이라 IncorrectResultSize 예외를 낸다")
    void fetchOnePropagatesIncorrectResultSize() {
        assertThatThrownBy(() -> query().where(Member::getStatus).equalTo("ACTIVE").fetchOne())
            .isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }

    @Test
    @DisplayName("count/exists 결과가 countByStatus('ACTIVE') 및 existsByRole('PARTNER')와 일치한다")
    void countAndExistsMatchRepository() {
        long expected = memberRepository.countByStatus("ACTIVE");
        long actual = query().where(Member::getStatus).equalTo("ACTIVE").count();
        assertThat(actual).isEqualTo(expected);

        boolean exists = memberRepository.existsByRole("PARTNER");
        assertThat(query().where(Member::getRole).equalTo("PARTNER").exists()).isEqualTo(exists);
    }
}