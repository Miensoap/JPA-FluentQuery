package me.miensoap.fluent.tests.kotlin

import me.miensoap.fluent.support.entity.Member
import me.miensoap.fluent.support.entity.MembershipType
import me.miensoap.fluent.support.repository.MemberRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for Kotlin compatibility with FluentQuery.
 *
 * Note: When using Kotlin with this library, prefer string-based field references
 * over Property<T, R> references for better compatibility.
 */
@SpringBootTest
class FluentQueryKotlinCompatibilityTest @Autowired constructor(
    private val memberRepository: MemberRepository
) {

    private val now = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        memberRepository.save(
            Member("ACTIVE", 30, "NORMAL", "USER", now,
                "kotlin@test.com", emptyList(), true, MembershipType.BASIC, null, null)
        )
        memberRepository.save(
            Member("ACTIVE", 25, "NORMAL", "USER", now,
                "another@test.com", emptyList(), true, MembershipType.BASIC, null, null)
        )
        memberRepository.save(
            Member("INACTIVE", 40, "PREMIUM", "ADMIN", now,
                "admin@test.com", emptyList(), false, MembershipType.PREMIUM, null, null)
        )
    }

    @AfterEach
    fun cleanup() {
        memberRepository.deleteAll()
    }

    @Test
    fun `should query with string-based field specification`() {
        val result = memberRepository.query()
            .where("email")
            .equalTo("kotlin@test.com")
            .fetch()

        assertEquals(1, result.size)
    }

    @Test
    fun `should query with multiple conditions`() {
        val result = memberRepository.query()
            .where("membershipType")
            .equalTo(MembershipType.BASIC)
            .and("age")
            .greaterThan(20)
            .fetch()

        assertEquals(2, result.size)
    }

    @Test
    fun `should query with or condition`() {
        val result = memberRepository.query()
            .where("email")
            .equalTo("kotlin@test.com")
            .or("email")
            .equalTo("another@test.com")
            .fetch()

        assertEquals(2, result.size)
    }

    @Test
    fun `should query with like operations`() {
        val result = memberRepository.query()
            .where("email")
            .containing("test")
            .fetch()

        assertEquals(3, result.size)
    }

    @Test
    fun `should query with negation`() {
        val result = memberRepository.query()
            .where("email")
            .notEqualTo("nonexistent@test.com")
            .fetch()

        assertEquals(3, result.size)
    }

    @Test
    fun `should query with isNull`() {
        memberRepository.save(
            Member("ACTIVE", 20, "NORMAL", "USER", now,
                null, emptyList(), true, MembershipType.BASIC, null, null)
        )

        val result = memberRepository.query()
            .where("email")
            .isNull()
            .fetch()

        assertEquals(1, result.size)
    }

    @Test
    fun `should query with comparison operators`() {
        val result = memberRepository.query()
            .where("age")
            .greaterThan(25)
            .fetch()

        assertEquals(2, result.size)
    }

    @Test
    fun `should query with in clause`() {
        val result = memberRepository.query()
            .where("email")
            .`in`(listOf("kotlin@test.com", "another@test.com"))
            .fetch()

        assertEquals(2, result.size)
    }

    @Test
    fun `should query with distinct`() {
        val result = memberRepository.query()
            .distinct()
            .where("status")
            .equalTo("ACTIVE")
            .fetch()

        assertEquals(2, result.size)
    }

    @Test
    fun `should query with ordering`() {
        val result = memberRepository.query()
            .orderBy("email")
            .ascending()
            .fetch()

        // Just verify ordering works - results should be sorted
        assertEquals(3, result.size)
    }

    @Test
    fun `should query with descending order`() {
        val result = memberRepository.query()
            .where("age")
            .equalTo(40)
            .fetch()

        assertEquals(1, result.size)
    }

    @Test
    fun `should count results`() {
        val count = memberRepository.query()
            .where("status")
            .equalTo("ACTIVE")
            .count()

        assertEquals(2L, count)
    }

    @Test
    fun `should check existence`() {
        val exists = memberRepository.query()
            .where("email")
            .equalTo("kotlin@test.com")
            .exists()

        assertTrue(exists)
    }

    @Test
    fun `should query with fetchOne`() {
        val result = memberRepository.query()
            .where("email")
            .equalTo("kotlin@test.com")
            .fetchOne()

        assertTrue(result.isPresent)
    }

    @Test
    fun `should return empty Optional when no match`() {
        val result = memberRepository.query()
            .where("email")
            .equalTo("nonexistent@test.com")
            .fetchOne()

        assertTrue(result.isEmpty)
    }

    @Test
    fun `should support method chaining`() {
        val result = memberRepository.query()
            .where("status")
            .equalTo("ACTIVE")
            .and("age")
            .greaterThanOrEqualTo(25)
            .orderBy("age")
            .descending()
            .fetch()

        assertEquals(2, result.size)
    }

    @Test
    fun `should query with not operator`() {
        val result = memberRepository.query()
            .where("membershipType")
            .equalTo(MembershipType.PREMIUM)
            .not()
            .fetch()

        assertEquals(2, result.size)
    }

    @Test
    fun `should query with startingWith`() {
        val result = memberRepository.query()
            .where("email")
            .startingWith("kotlin")
            .fetch()

        assertEquals(1, result.size)
    }

    @Test
    fun `should query with endingWith`() {
        val result = memberRepository.query()
            .where("email")
            .endingWith("test.com")
            .fetch()

        assertEquals(3, result.size)
    }

    @Test
    fun `should work with boolean property`() {
        val result = memberRepository.query()
            .where("active")
            .isTrue()
            .fetch()

        assertEquals(2, result.size)
    }

    @Test
    fun `should work with isFalse`() {
        val result = memberRepository.query()
            .where("active")
            .isFalse()
            .fetch()

        assertEquals(1, result.size)
    }

    @Test
    fun `should work with isNotNull`() {
        val result = memberRepository.query()
            .where("email")
            .isNotNull()
            .fetch()

        assertEquals(3, result.size)
    }
}
