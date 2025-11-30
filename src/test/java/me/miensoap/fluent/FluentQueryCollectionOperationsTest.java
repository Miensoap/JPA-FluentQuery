package me.miensoap.fluent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FluentQueryCollectionOperationsTest extends AbstractFluentQueryIntegrationTest {

    @Test
    @DisplayName("role.in(빈) 은 빈 결과, role.notIn(빈) 은 전체 결과를 반환한다")
    void inAndNotInSupportEmptyCollections() {
        List<Member> none = query().where(Member::getRole).in(List.of()).fetch();
        assertThat(none).isEmpty();

        List<Member> everyone = query().where(Member::getRole).notIn(List.of()).fetch();
        assertThat(everyone).hasSize(Math.toIntExact(memberRepository.count()));
    }

    @Test
    @DisplayName("role.in(null) 호출 시 IllegalArgumentException 을 던진다")
    void nullCollectionsAreRejected() {
        assertThatThrownBy(() -> query().where(Member::getRole).in(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("in");
    }

    @Test
    @DisplayName("notIn/tags.isEmpty/isNotEmpty 가 실제 필드 값과 일치한다")
    void collectionPredicatesMatchDerivedResults() {
        List<Member> notIn = query().where(Member::getRole).notIn(List.of("USER", "STAFF")).fetch();
        assertThat(notIn).allMatch(member -> !List.of("USER", "STAFF").contains(member.getRole()));

        List<Member> emptyTags = query().where(Member::getTags).isEmpty().fetch();
        assertThat(emptyTags).allMatch(member -> member.getTags().isEmpty());

        List<Member> notEmptyTags = query().where(Member::getTags).isNotEmpty().fetch();
        assertThat(notEmptyTags).allMatch(member -> !member.getTags().isEmpty());
    }

    @Test
    @DisplayName("active.isTrue/isFalse 가 TRUE/FALSE 멤버만 걸러낸다")
    void booleanPredicatesMapCorrectly() {
        List<Member> active = query().where(Member::getActive).isTrue().fetch();
        assertThat(active).allMatch(member -> Boolean.TRUE.equals(member.getActive()));

        List<Member> inactive = query().where(Member::getActive).isFalse().fetch();
        assertThat(inactive).allMatch(member -> Boolean.FALSE.equals(member.getActive()));
    }
}
