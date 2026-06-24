package com.demo.gym.member;

import com.demo.gym.gym.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByTaxId(String taxId);

    boolean existsByTaxId(String taxId);

    @Query("SELECT hp.gym FROM GymMember hp WHERE hp.member.id = :memberId")
    List<Gym> findGymsByMemberId(Long memberId);
}