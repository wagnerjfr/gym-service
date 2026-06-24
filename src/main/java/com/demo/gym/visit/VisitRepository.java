package com.demo.gym.visit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findByMemberId(Long memberId);

    List<Visit> findByMemberIdAndStatus(Long memberId, VisitStatus status);

    List<Visit> findByGymId(Long gymId);

    List<Visit> findByGymIdAndStatus(Long gymId, VisitStatus status);

    @Query("SELECT s FROM Visit s WHERE s.member.id = :memberId " +
            "AND s.checkIn >= :startDate AND s.checkIn < :endDate")
    List<Visit> findByMemberIdAndDateRange(Long memberId, LocalDateTime startDate, LocalDateTime endDate);

    // Simpler alternative:
    boolean existsByGymIdAndMemberIdAndStatus(Long gymId, Long memberId, VisitStatus status);

    @Query("SELECT s FROM Visit s JOIN FETCH s.gym JOIN FETCH s.member WHERE s.id = :id")
    Optional<Visit> findByIdWithGymAndMember(Long id);

    @Query("SELECT s FROM Visit s JOIN FETCH s.gym JOIN FETCH s.member WHERE s.member.id = :memberId")
    List<Visit> findByMemberIdWithGymAndMember(Long memberId);

    @Query("SELECT s FROM Visit s JOIN FETCH s.gym JOIN FETCH s.member WHERE s.gym.id = :gymId")
    List<Visit> findByGymIdWithGymAndMember(Long gymId);

    List<Visit> findByBillId(Long billId);

    // New
    @Query("SELECT s FROM Visit s WHERE s.member.id = :memberId " +
            "AND s.checkIn < :windowEnd " +
            "AND (s.checkOut IS NULL OR s.checkOut > :windowStart)")
    List<Visit> findByMemberIdAndOverlappingWindow(Long memberId, Instant windowStart, Instant windowEnd);
}
