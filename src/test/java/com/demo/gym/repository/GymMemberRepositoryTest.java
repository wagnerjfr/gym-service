package com.demo.gym.repository;
import com.demo.gym.member.GymMemberRepository;
import com.demo.gym.gym.GymRepository;
import com.demo.gym.member.MemberRepository;

import com.demo.gym.GymApplication;
import com.demo.gym.common.Address;
import com.demo.gym.gym.Gym;
import com.demo.gym.member.GymMember;
import com.demo.gym.member.Member;
import com.demo.gym.member.BiologicalSex;
import com.demo.gym.gym.GymType;
import com.demo.gym.member.MembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
//@ContextConfiguration(classes = GymApplication.class)
//@EnableJpaRepositories(basePackages = {"com.demo.gym.member", "com.demo.gym.gym"})
//@EntityScan(basePackages = {"com.demo.gym.visit", "com.demo.gym.gym", "com.demo.gym.member", "com.demo.gym.billing", "com.demo.gym.common"})
class GymMemberRepositoryTest {

    @Autowired
    private GymMemberRepository gymMemberRepository;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Gym testGym;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testGym = createTestGym();
        testMember = createTestMember();
    }

    @Test
    void shouldFindMembersByGymId() {
        Gym gym = gymRepository.save(testGym);
        Member member = memberRepository.save(testMember);

        GymMember registration = createRegistration(gym, member);
        gymMemberRepository.save(registration);

        assertThat(registration.getId()).isNotNull();
        assertThat(registration.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

        List<GymMember> resultsByGym = gymMemberRepository.findByGymId(gym.getId());

        assertThat(resultsByGym).hasSize(1);
        assertThat(resultsByGym.get(0).getMember().getName()).isEqualTo("Test Member");

        List<GymMember> resultsByMember = gymMemberRepository.findByMemberId(member.getId());

        assertThat(resultsByMember).hasSize(1);
        assertThat(resultsByMember.get(0).getGym().getName()).isEqualTo("Test Gym");

        boolean exists = gymMemberRepository.existsByGymIdAndMemberId(
                gym.getId(), member.getId());

        assertThat(exists).isTrue();

    }

    @Test
    void shouldReturnFalseWhenRegistrationNotExists() {
        boolean exists = gymMemberRepository.existsByGymIdAndMemberId(999L, 999L);

        assertThat(exists).isFalse();
    }

    @Test
    void shouldAllowMultipleRegistrationsForSameMember() {
        Gym gym1 = gymRepository.save(testGym);

        Gym gym2 = createTestGym();
        ReflectionTestUtils.setField(gym2, "taxId", "TAX999");
        gym2.setName("Second Gym");
        gym2 = gymRepository.save(gym2);

        Member member = memberRepository.save(testMember);

        gymMemberRepository.save(createRegistration(gym1, member));
        gymMemberRepository.save(createRegistration(gym2, member));

        List<GymMember> registrations = gymMemberRepository.findByMemberId(member.getId());

        assertThat(registrations).hasSize(2);
    }

    @Test
    void shouldAllowMultipleRegistrationsForSameGym() {
        Member member1 = memberRepository.save(testMember);

        Member member2 = createTestMember();
        ReflectionTestUtils.setField(member2, "taxId", "TAX999");
        member2.setName("Second Member");
        member2 = memberRepository.save(member2);

        Gym gym = gymRepository.save(testGym);

        gymMemberRepository.save(createRegistration(gym, member1));
        gymMemberRepository.save(createRegistration(gym, member2));

        List<GymMember> registrations = gymMemberRepository.findByGymId(gym.getId());
        assertThat(registrations).hasSize(2);
    }

    private Gym createTestGym() {
        Gym h = new Gym();
        h.setName("Test Gym");
        h.setTaxId("TAX123");
        h.setEmail("test@gym.com");
        h.setPhoneNumber("123-456");
        h.setWebsiteUrl("www.test.com");
        h.setCapacity(100);
        h.setType(GymType.PUBLIC);
        h.setAddress(new Address("Germany", "Munich", "123 Main", "02101"));
        return h;
    }

    private Member createTestMember() {
        Member p = new Member();
        p.setName("Test Member");
        p.setTaxId("PAT123");
        p.setEmail("test@member.com");
        p.setPhoneNumber("555-1234");
        p.setBirthDate(LocalDate.of(1990, 1, 15));
        p.setSex(BiologicalSex.MALE);
        p.setAddress(new Address("Germany", "Berlin", "456 Oak", "02102"));
        return p;
    }

    private GymMember createRegistration(Gym gym, Member member) {
        GymMember hp = new GymMember();

        GymMember.GymMemberId id = new GymMember.GymMemberId();
        id.setGymId(gym.getId());
        id.setMemberId(member.getId());
        hp.setId(id);

        hp.setGym(gym);
        hp.setMember(member);
        hp.setStatus(MembershipStatus.ACTIVE);
        hp.setRegisteredAt(LocalDateTime.now());
        return hp;
    }
}
