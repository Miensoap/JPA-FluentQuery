package me.miensoap.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.miensoap.fluent.entity.Member;

class FluentQueryTemporalOperationsTest extends
    AbstractFluentQueryIntegrationTest {

    @Test
    @DisplayName("between/after/before 가 지정한 LocalDateTime 구간만 반환한다")
    void betweenAfterBeforeCoverEdgeCases() {
        LocalDateTime lower = now.minusDays(5);
        LocalDateTime upper = now.minusDays(1);

        List<Member> between = query().where(Member::getLastLoginAt).between(lower, upper).fetch();
        assertThat(between).allMatch(member -> !member.getLastLoginAt().isBefore(lower) && !member.getLastLoginAt().isAfter(upper));

        List<Member> after = query().where(Member::getLastLoginAt).after(upper).fetch();
        assertThat(after).allMatch(member -> member.getLastLoginAt().isAfter(upper));

        List<Member> before = query().where(Member::getLastLoginAt).before(lower).fetch();
        assertThat(before).allMatch(member -> member.getLastLoginAt().isBefore(lower));
    }
}
