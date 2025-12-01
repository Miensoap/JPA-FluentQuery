package me.miensoap.fluent.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;
    private Integer age;
    private String grade;
    private String role;
    private LocalDateTime lastLoginAt;
    private String email;
    private Boolean active;

    @Enumerated(EnumType.STRING)
    private MembershipType membershipType;

    @ElementCollection
    @Getter(AccessLevel.NONE)
    private List<String> tags = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Team team;

    @Embedded
    private Address address;

    public Member(String status, Integer age, String grade, String role, LocalDateTime lastLoginAt) {
        this(status, age, grade, role, lastLoginAt, null, List.of(), null, null, null, null);
    }

    public Member(String status, Integer age, String grade, String role, LocalDateTime lastLoginAt,
                  String email, List<String> tags) {
        this(status, age, grade, role, lastLoginAt, email, tags, null, null, null, null);
    }

    public Member(String status, Integer age, String grade, String role, LocalDateTime lastLoginAt,
                  String email, List<String> tags, Boolean active, MembershipType membershipType,
                  Team team, Address address) {
        this.status = status;
        this.age = age;
        this.grade = grade;
        this.role = role;
        this.lastLoginAt = lastLoginAt;
        this.email = email;
        this.tags = new ArrayList<>(tags);
        this.active = active;
        this.membershipType = membershipType;
        this.team = team;
        this.address = address;
    }
    public List<String> getTags() {
        return new ArrayList<>(tags);
    }
}
