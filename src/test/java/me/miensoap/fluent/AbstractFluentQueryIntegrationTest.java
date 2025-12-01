package me.miensoap.fluent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import me.miensoap.fluent.core.FluentQuery;
import me.miensoap.fluent.entity.Address;
import me.miensoap.fluent.entity.Member;
import me.miensoap.fluent.entity.MembershipType;
import me.miensoap.fluent.entity.Team;
import me.miensoap.fluent.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest(properties = {
    "spring.jpa.properties.hibernate.session_factory.statement_inspector=me.miensoap.fluent.support.CapturingStatementInspector"
})
@Transactional
abstract class AbstractFluentQueryIntegrationTest {

    @Autowired
    protected MemberRepository memberRepository;

    @PersistenceContext
    protected EntityManager entityManager;

    protected Team devTeam;
    protected Team opsTeam;
    protected Team partnerTeam;
    protected LocalDateTime now;

    @BeforeEach
    void baseSetUp() {
        now = LocalDateTime.now();
        memberRepository.deleteAll();
        devTeam = new Team("Developers", "DEV");
        opsTeam = new Team("Operations", "OPS");
        partnerTeam = new Team("Partners", "PRT");
        memberRepository.saveAll(seedMembers());
        clearExecutedSql();
    }

    protected FluentQuery<Member> query() {
        return memberRepository.query();
    }

    protected List<Long> ids(List<Member> members) {
        return members.stream()
            .map(Member::getId)
            .sorted()
            .collect(Collectors.toList());
    }

    protected void clearExecutedSql() {
        me.miensoap.fluent.support.CapturingStatementInspector.clear();
    }

    protected List<String> executedSql() {
        return me.miensoap.fluent.support.CapturingStatementInspector.statements();
    }

    private List<Member> seedMembers() {
        return List.of(
            member("ACTIVE", 35, "VIP", "USER", now.minusDays(1), "alice@corp.com", List.of("vip", "legacy"), true, MembershipType.VIP, devTeam, new Address("Seoul", "KR")),
            member("ACTIVE", 28, "BASIC", "ANALYST", now.minusDays(2), "amanda@corp.com", List.of(), true, MembershipType.BASIC, devTeam, new Address("Busan", "KR")),
            member("INACTIVE", 40, "VIP_GOLD", "ADMIN", now.minusDays(10), "isaac@corp.com", List.of("internal"), false, MembershipType.VIP_GOLD, opsTeam, new Address("Seoul", "KR")),
            member("ACTIVE", 20, "BASIC", "STAFF", now.minusDays(5), "bob@corp.com", List.of(), true, MembershipType.BASIC, opsTeam, new Address("Tokyo", "JP")),
            member("SUSPENDED", 33, "PREMIUM", "PARTNER", now.minusDays(3), "sue@partner.com", List.of("partner", "beta"), false, MembershipType.PREMIUM, partnerTeam, new Address("New York", "US")),
            member("DELETED", 30, "BASIC", "VISITOR", now.minusHours(6), null, List.of("legacy"), null, MembershipType.BASIC, partnerTeam, new Address("Seoul", "KR"))
        );
    }

    private Member member(String status, Integer age, String grade, String role, LocalDateTime lastLoginAt,
                          String email, List<String> tags, Boolean active, MembershipType type,
                          Team team, Address address) {
        return new Member(status, age, grade, role, lastLoginAt, email, tags, active, type, team, address);
    }
}
