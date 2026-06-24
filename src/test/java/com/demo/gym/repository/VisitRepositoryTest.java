package com.demo.gym.repository;
import com.demo.gym.visit.VisitRepository;
import com.demo.gym.gym.GymRepository;
import com.demo.gym.member.MemberRepository;
import com.demo.gym.billing.BillRepository;

import static org.assertj.core.api.Assertions.assertThat;

import com.demo.gym.GymApplication;
import com.demo.gym.billing.Bill;
import com.demo.gym.gym.Gym;
import com.demo.gym.member.Member;
import com.demo.gym.visit.Visit;
import com.demo.gym.billing.BillStatus;
import com.demo.gym.visit.VisitStatus;
import com.demo.gym.util.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.List;

@DataJpaTest
@ContextConfiguration(classes = GymApplication.class)
@EnableJpaRepositories(basePackages = {"com.demo.gym.visit", "com.demo.gym.gym", "com.demo.gym.member", "com.demo.gym.billing"})
@EntityScan(basePackages = {"com.demo.gym.visit", "com.demo.gym.gym", "com.demo.gym.member", "com.demo.gym.billing", "com.demo.gym.common"})
public class VisitRepositoryTest {

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BillRepository billRepository;

    private Gym testGym;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testGym = gymRepository.save(TestEntityFactory.createTestGym("TAX001", "Test Gym"));
        testMember = memberRepository.save(TestEntityFactory.createTestMember("PAT001", "Test Member"));
    }

    @Test
    void shouldCompleteActiveVisit() {
        Bill bill = new Bill();
        bill.setGym(testGym);
        bill.setMember(testMember);
        bill.setStatus(BillStatus.OPEN);
        Bill savedBill = billRepository.save(bill);

        Visit visit = TestEntityFactory.createTestVisit(testGym, testMember, VisitStatus.CHECKED_IN);
        visit.setBill(savedBill);

        Visit savedVisit = visitRepository.save(visit);

        Visit stayToComplete = visitRepository.findById(savedVisit.getId()).orElseThrow();
        stayToComplete.setStatus(VisitStatus.COMPLETED);
        stayToComplete.setCheckOut(Instant.now());
        visitRepository.save(stayToComplete);

        List<Visit> activeVisits = visitRepository.findByMemberIdAndStatus(
                testMember.getId(), VisitStatus.CHECKED_IN);
        List<Visit> completedVisits = visitRepository.findByMemberIdAndStatus(
                testMember.getId(), VisitStatus.COMPLETED);

        assertThat(activeVisits).isEmpty();
        assertThat(completedVisits).hasSize(1);
    }

    @Test
    void shouldDischargeActiveVisit() {
        Visit savedVisit = visitRepository.save(
                TestEntityFactory.createTestVisit(testGym, testMember, VisitStatus.CHECKED_IN));

        Visit stayToDischarge = visitRepository.findById(savedVisit.getId()).orElseThrow();
        stayToDischarge.setStatus(VisitStatus.CHECKED_OUT);
        stayToDischarge.setCheckOut(Instant.now());
        visitRepository.save(stayToDischarge);

        List<Visit> dischargedVisits = visitRepository.findByMemberIdAndStatus(
                testMember.getId(), VisitStatus.CHECKED_OUT);

        assertThat(dischargedVisits).hasSize(1);
    }

    @Test
    void shouldExistsByGymIdAndMemberIdAndStatus() {
        visitRepository.save(
                TestEntityFactory.createTestVisit(testGym, testMember, VisitStatus.CHECKED_IN));

        boolean exists = visitRepository.existsByGymIdAndMemberIdAndStatus(
                testGym.getId(), testMember.getId(), VisitStatus.CHECKED_IN);

        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNoActiveVisitExists() {
        visitRepository.save(
                TestEntityFactory.createTestVisit(testGym, testMember, VisitStatus.COMPLETED));

        boolean exists = visitRepository.existsByGymIdAndMemberIdAndStatus(
                testGym.getId(), testMember.getId(), VisitStatus.CHECKED_IN);

        assertThat(exists).isFalse();
    }
}