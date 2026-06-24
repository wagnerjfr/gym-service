package com.demo.gym.member;

import com.demo.gym.member.GymMember.GymMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GymMemberRepository extends JpaRepository<GymMember, GymMemberId> {

    List<GymMember> findByGymId(Long gymId);

    List<GymMember> findByMemberId(Long memberId);

    boolean existsByGymIdAndMemberId(Long gymId, Long memberId);

}
