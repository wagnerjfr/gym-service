package com.demo.gym.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findFirstByMemberIdAndGymIdAndStatus(Long memberId, Long gymId, BillStatus status);

    List<Bill> findByMemberId(Long memberId);
}
