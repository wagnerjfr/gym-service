package com.demo.gym.gym;

import com.demo.gym.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GymRepository extends JpaRepository<Gym, Long> {

    Optional<Gym> findByTaxId(String taxId);

    boolean existsByTaxId(String taxId);

    @Query("SELECT hp.member FROM GymMember hp WHERE hp.gym.id = :gymId")
    List<Member> findMembersByGymId(Long gymId);
}
