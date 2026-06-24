package com.demo.gym.member;

import com.demo.gym.gym.Gym;
import com.demo.gym.gym.GymRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GymMemberService {

    private final GymMemberRepository gymMemberRepository;
    private final GymRepository gymRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public GymMember registerMember(Long gymId, Long memberId) {
        if (gymMemberRepository.existsByGymIdAndMemberId(gymId, memberId)) {
            throw new IllegalStateException("Member already registered in this gym");
        }

        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new EntityNotFoundException("Gym not found"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        GymMember registration = new GymMember();

        GymMember.GymMemberId id = new GymMember.GymMemberId();
        id.setGymId(gym.getId());
        id.setMemberId(member.getId());
        registration.setId(id);

        registration.setGym(gym);
        registration.setMember(member);
        registration.setStatus(MembershipStatus.ACTIVE);
        registration.setRegisteredAt(LocalDateTime.now());

        return gymMemberRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public List<Member> getGymMembers(Long gymId) {
        return gymMemberRepository.findByGymId(gymId)
                .stream()
                .map(GymMember::getMember)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Gym> getMemberGyms(Long memberId) {
        return gymMemberRepository.findByMemberId(memberId)
                .stream()
                .map(GymMember::getGym)
                .toList();
    }
}
