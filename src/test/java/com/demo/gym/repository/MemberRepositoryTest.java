package com.demo.gym.repository;
import com.demo.gym.member.MemberRepository;

import com.demo.gym.common.Address;
import com.demo.gym.member.Member;
import com.demo.gym.GymApplication;
import com.demo.gym.member.BiologicalSex;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ContextConfiguration(classes = GymApplication.class)
@EnableJpaRepositories(basePackages = {"com.demo.gym.member"})
@EntityScan(basePackages = {"com.demo.gym.visit", "com.demo.gym.gym", "com.demo.gym.member", "com.demo.gym.billing", "com.demo.gym.common"})
public class MemberRepositoryTest {

    @Autowired
    MemberRepository repository;

    @Test
    void shouldSaveAndRetrieveMember() {
        final String taxId = "1111";
        final String name = "Test Member";

        Member member = createValidMember(taxId, name);

        Member savedMember = repository.save(member);

        assertThat(savedMember.getId()).isNotNull();
        assertThat(savedMember.getName()).isEqualTo(name);

        Member found = repository.findById(savedMember.getId()).orElseThrow();
        assertThat(found.getTaxId()).isEqualTo(taxId);
    }

    @Test
    void shouldThrowExceptionWhenMemberTaxIdIsDuplicate() {
        final String taxId = "1112";

        Member p1 = createValidMember(taxId, "Member 1");
        repository.saveAndFlush(p1);

        Member p2 = createValidMember(taxId, "Member 2");

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(p2));
    }

    @Test
    void shouldFailWhenNameIsNull() {
        Member member = createValidMember("111", null);

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(member));
    }

    private Member createValidMember(String taxId, String name) {
        var member = new Member();
        member.setTaxId(taxId);
        member.setName(name);
        member.setAddress(new Address("country", "city", "street", "zipCode"));
        member.setEmail("member@gmail.com");
        member.setPhoneNumber("123456");
        member.setBirthDate(LocalDate.of(1960, Month.JUNE, 15));
        member.setSex(BiologicalSex.FEMALE);
        return member;
    }
}
