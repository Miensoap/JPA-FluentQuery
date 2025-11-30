package me.miensoap.fluent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FluentQueryStringOperationsTest extends
    AbstractFluentQueryIntegrationTest {

    @Test
    @DisplayName("grade LIKE 'VIP%' 와 email NOT LIKE '%partner.com' 조건을 검증한다")
    void likeAndNotLikeMatchExpectations() {
        List<Member> likeResult = query().where(Member::getGrade).like("VIP%").fetch();
        assertThat(likeResult).allMatch(member -> member.getGrade().startsWith("VIP"));

        List<Member> notLikeResult = query().where(Member::getEmail).notLike("%partner.com").fetch();
        assertThat(notLikeResult).allMatch(member -> member.getEmail() == null || !member.getEmail().endsWith("partner.com"));
    }

    @Test
    @DisplayName("status CONTAINING 'ACT' 및 IGNORE CASE 변형이 예상 문자열을 찾는다")
    void containingVariantsBehaveCorrectly() {
        List<Member> containing = query().where(Member::getStatus).containing("ACT").fetch();
        assertThat(containing).allMatch(member -> member.getStatus().contains("ACT"));

        List<Member> ignoreCase = query().where(Member::getStatus).containingIgnoreCase("spend").fetch();
        assertThat(ignoreCase).allMatch(member -> member.getStatus().toLowerCase().contains("spend"));

        List<Member> endingIgnoreCase = query().where(Member::getGrade).endingWithIgnoreCase("gold").fetch();
        assertThat(endingIgnoreCase).allMatch(member -> member.getGrade().toLowerCase().endsWith("gold"));
    }

    @Test
    @DisplayName("grade.like(null) / containingIgnoreCase(null) 는 IllegalArgumentException 을 던진다")
    void nullArgumentRaisesHelpfulError() {
        assertThatThrownBy(() -> query().where(Member::getGrade).like(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("like");

        assertThatThrownBy(() -> query().where(Member::getGrade).containingIgnoreCase(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("containingIgnoreCase");
    }

    @Test
    @DisplayName("Boolean 필드에 문자열 연산을 호출하면 명확한 예외를 던진다")
    void stringOperationsOnBooleanPropertiesThrow() {
        assertThatThrownBy(() -> query().where(Member::getActive).containing("true"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("String property")
            .hasMessageContaining("active (java.lang.Boolean)");
    }

    @Test
    @DisplayName("team.departmentCode 와 address.city 경로 필터가 정상 동작한다")
    void nestedPathResolvesToJoinAttribute() {
        List<Member> devs = query().where("team.departmentCode").equalTo("DEV").fetch();
        assertThat(devs).allMatch(member -> member.getTeam().getDepartmentCode().equals("DEV"));

        List<Member> seoul = query().where("address.city").equalTo("Seoul").fetch();
        assertThat(seoul).allMatch(member -> member.getAddress().getCity().equals("Seoul"));
    }
}
