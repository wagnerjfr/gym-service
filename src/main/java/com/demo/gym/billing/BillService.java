package com.demo.gym.billing;

import com.demo.gym.gym.Gym;
import com.demo.gym.member.Member;
import com.demo.gym.visit.Visit;
import com.demo.gym.gym.GymRepository;
import com.demo.gym.member.MemberRepository;
import com.demo.gym.visit.VisitRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillService {

    public static final String EUR_CURRENCY = "EUR";

    private final BillRepository billRepository;
    private final GymRepository gymRepository;
    private final MemberRepository memberRepository;
    private final VisitRepository visitRepository;

    @Transactional
    public Bill createBill(Long gymId, Long memberId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new EntityNotFoundException("Gym not found with id: " + gymId));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + memberId));

        Bill bill = new Bill();
        bill.setGym(gym);
        bill.setMember(member);
        bill.setStatus(BillStatus.OPEN);
        bill.setCurrency(EUR_CURRENCY);
        bill.setTotalAmount(BigDecimal.ZERO);
        bill.setIssueDate(Instant.now());
        bill.setCreatedAt(Instant.now());
        bill.setUpdatedAt(Instant.now());

        return billRepository.save(bill);
    }

    @Transactional
    public Bill getOrCreateOpenBill(Long gymId, Long memberId) {
        return billRepository.findFirstByMemberIdAndGymIdAndStatus(memberId, gymId, BillStatus.OPEN)
                .orElseGet(() -> createBill(gymId, memberId));
    }

    @Transactional
    public Bill closeBill(Long billId) {
        Bill bill = getBill(billId);
        if (bill.getStatus() == BillStatus.CLOSED) {
            return bill;
        }

        bill.setTotalAmount(recalculateTotalAmount(billId));
        bill.setStatus(BillStatus.CLOSED);
        bill.setClosedAt(Instant.now());
        bill.setUpdatedAt(Instant.now());

        return billRepository.save(bill);
    }

    @Transactional(readOnly = true)
    public Bill getBill(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bill not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Bill> getMemberBills(Long memberId) {
        return billRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getOutstandingBalance(Long memberId, Long gymId) {
        return billRepository.findFirstByMemberIdAndGymIdAndStatus(memberId, gymId, BillStatus.OPEN)
                .map(openBill -> recalculateTotalAmount(openBill.getId()))
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal recalculateTotalAmount(Long billId) {
        return visitRepository.findByBillId(billId).stream()
                .filter(visit -> visit.getCheckIn() != null)
                .filter(Visit::isBillable)
                .map(visit -> {
                    Instant end = visit.getCheckOut() != null ? visit.getCheckOut() : Instant.now();
                    long days = Math.max(1, Duration.between(visit.getCheckIn(), end).toDays());
                    BigDecimal dailyRate = visit.getDailyRate() != null ? visit.getDailyRate() : BigDecimal.ZERO;
                    return dailyRate.multiply(BigDecimal.valueOf(days));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
