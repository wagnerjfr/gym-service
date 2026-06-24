package com.demo.gym.visit;

import com.demo.gym.billing.BillService;
import com.demo.gym.gym.GymRepository;
import com.demo.gym.member.GymMemberRepository;
import com.demo.gym.member.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class VisitService {

    public record VisitSummaryQuarter(long totalDays, BigDecimal billedAmount, String currency) {}

    private final VisitRepository visitRepository;
    private final GymRepository gymRepository;
    private final MemberRepository memberRepository;
    private final GymMemberRepository gymMemberRepository;
    private final BillService billService;

    @Transactional
    public Visit createVisit(Long gymId, Long memberId, Instant checkIn,
                             String zone, String stationCode, BigDecimal dailyRate) {
        gymRepository.findById(gymId)
                .orElseThrow(() -> new EntityNotFoundException("Gym not found with id: " + gymId));

        memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + memberId));

        if (!gymMemberRepository.existsByGymIdAndMemberId(gymId, memberId)) {
            throw new IllegalStateException("Member must be registered at gym before check-in");
        }

        if (visitRepository.existsByGymIdAndMemberIdAndStatus(gymId, memberId, VisitStatus.CHECKED_IN)) {
            throw new IllegalStateException("Member already has a checked-in visit at this gym");
        }

        Visit visit = new Visit();
        visit.setGym(gymRepository.getReferenceById(gymId));
        visit.setMember(memberRepository.getReferenceById(memberId));
        visit.setBill(billService.getOrCreateOpenBill(gymId, memberId));
        visit.setCheckIn(checkIn != null ? checkIn : Instant.now());
        visit.setZone(zone);
        visit.setStationCode(stationCode);
        visit.setDailyRate(dailyRate);
        visit.setStatus(VisitStatus.CHECKED_IN);
        visit.setCreatedAt(Instant.now());

        return visitRepository.save(visit);
    }

    @Transactional
    public Visit cancelVisit(Long visitId, String reason) {
        Visit visit = getVisitOrThrow(visitId);

        if (visit.getStatus() != VisitStatus.CHECKED_IN) {
            throw new IllegalStateException("Only CHECKED_IN visits can be cancelled");
        }

        visit.setStatus(VisitStatus.CANCELLED);
        visit.setNote(reason);
        visit.setCheckOut(Instant.now());
        visit.setBillable(false);

        return visitRepository.save(visit);
    }

    @Transactional
    public Visit completeVisit(Long visitId, Instant endDate) {
        Visit visit = getVisitOrThrow(visitId);

        if (visit.getStatus() != VisitStatus.CHECKED_IN) {
            throw new IllegalStateException("Only CHECKED_IN visits can be completed");
        }

        visit.setStatus(VisitStatus.COMPLETED);
        visit.setCheckOut(endDate != null ? endDate : Instant.now());

        return visitRepository.save(visit);
    }

    @Transactional
    public Visit checkOutVisit(Long visitId, Instant checkOut) {
        Visit visit = getVisitOrThrow(visitId);

        if (visit.getStatus() != VisitStatus.CHECKED_IN && visit.getStatus() != VisitStatus.CANCELLED) {
            throw new IllegalStateException("Only CHECKED_IN or CANCELLED visits can be checked out");
        }

        visit.setStatus(VisitStatus.CHECKED_OUT);
        visit.setCheckOut(checkOut != null ? checkOut : Instant.now());

        if (visit.getBill() != null && visit.getBill().getId() != null) {
            billService.closeBill(visit.getBill().getId());
        }

        return visitRepository.save(visit);
    }

    @Transactional(readOnly = true)
    public Visit getVisit(Long visitId) {
        return getVisitOrThrow(visitId);
    }

    @Transactional(readOnly = true)
    public List<Visit> getMemberVisits(Long memberId, VisitStatus statusFilter) {
        List<Visit> visits = visitRepository.findByMemberIdWithGymAndMember(memberId);
        if (statusFilter != null) {
            return visits.stream().filter(s -> s.getStatus() == statusFilter).toList();
        }
        return visits;
    }

    @Transactional(readOnly = true)
    public List<Visit> getGymVisits(Long gymId, VisitStatus statusFilter) {
        List<Visit> visits = visitRepository.findByGymIdWithGymAndMember(gymId);
        if (statusFilter != null) {
            return visits.stream().filter(s -> s.getStatus() == statusFilter).toList();
        }
        return visits;
    }

    @Transactional(readOnly = true)
    public VisitSummaryQuarter getVisitSummaryQuarter(Long memberId, Long gymId, int year, int quarter) {
        int startMonth = ((quarter - 1) * 3) + 1;

        Instant windowStart = LocalDate.of(year, startMonth, 1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        Instant windowEnd = LocalDate.of(year, startMonth, 1)
                .plusMonths(3)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        List<Visit> visits = visitRepository.findByMemberIdAndOverlappingWindow(memberId, windowStart, windowEnd);

        long totalDays = 0;
        BigDecimal billedAmount = BigDecimal.ZERO;

        for (Visit visit : visits) {
            if (gymId != null && gymId > 0 && !gymId.equals(visit.getGym().getId())) {
                continue;
            }
            if (!visit.isBillable() || visit.getCheckIn() == null) {
                continue;
            }

            Instant overlapStart = visit.getCheckIn().isAfter(windowStart) ? visit.getCheckIn() : windowStart;
            Instant visitEnd = visit.getCheckOut() != null ? visit.getCheckOut() : Instant.now();
            Instant overlapEnd = visitEnd.isBefore(windowEnd) ? visitEnd : windowEnd;

            if (!overlapEnd.isAfter(overlapStart)) {
                continue;
            }

            long days = Duration.between(overlapStart, overlapEnd).toDays();
            if (days < 0) {
                continue;
            }
            long billedDays = Math.max(1, days);
            totalDays += billedDays;
            BigDecimal dailyRate = visit.getDailyRate() != null ? visit.getDailyRate() : BigDecimal.ZERO;
            billedAmount = billedAmount.add(dailyRate.multiply(BigDecimal.valueOf(billedDays)));
        }

        return new VisitSummaryQuarter(totalDays, billedAmount.setScale(2, RoundingMode.HALF_UP), BillService.EUR_CURRENCY);
    }

    private Visit getVisitOrThrow(Long visitId) {
        return visitRepository.findByIdWithGymAndMember(visitId)
                .orElseThrow(() -> new EntityNotFoundException("Visit not found with id: " + visitId));
    }
}
