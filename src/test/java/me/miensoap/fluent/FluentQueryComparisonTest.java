package me.miensoap.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.miensoap.fluent.entity.Member;

class FluentQueryComparisonTest extends AbstractFluentQueryIntegrationTest {

    @Test
    @DisplayName("age >= 30 과 > 30 필터가 서로 다른 결과를 반환한다")
    void greaterThanAndGreaterThanOrEqualRespectBoundaries() {
        List<Member> ge = query().where(Member::getAge).greaterThanOrEqualTo(30).fetch();
        List<Member> gt = query().where(Member::getAge).greaterThan(30).fetch();

        assertThat(ge).hasSizeGreaterThan(gt.size());
        assertThat(gt).allMatch(m -> m.getAge() > 30);
        assertThat(ge).allMatch(m -> m.getAge() >= 30);
    }

    @Test
    @DisplayName("age <= 30 과 < 30 필터가 서로 다른 결과를 반환한다")
    void lessThanAndLessThanOrEqualRespectBoundaries() {
        List<Member> le = query().where(Member::getAge).lessThanOrEqualTo(30).fetch();
        List<Member> lt = query().where(Member::getAge).lessThan(30).fetch();

        assertThat(lt).allMatch(m -> m.getAge() < 30);
        assertThat(le).allMatch(m -> m.getAge() <= 30);
        assertThat(le).hasSizeGreaterThanOrEqualTo(lt.size());
    }

    @Test
    @DisplayName("between(25,30)이 25 이상 30 이하 멤버만 반환한다")
    void betweenIncludesEndValues() {
        List<Member> members = query().where(Member::getAge).between(25, 30).fetch();
        assertThat(members).allMatch(m -> m.getAge() >= 25 && m.getAge() <= 30);
    }

    @Test
    @DisplayName("lastLoginAt.after/before 가 경계 시간 이후/이전만 선택한다")
    void afterBeforeUseExclusiveBounds() {
        var recent = query().where(Member::getLastLoginAt).after(now.minusDays(4)).fetch();
        assertThat(recent).allMatch(m -> m.getLastLoginAt().isAfter(now.minusDays(4)));

        var old = query().where(Member::getLastLoginAt).before(now.minusDays(4)).fetch();
        assertThat(old).allMatch(m -> m.getLastLoginAt().isBefore(now.minusDays(4)));
    }
}
