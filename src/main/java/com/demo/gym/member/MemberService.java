package com.demo.gym.member;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public Member saveMember(Member member) {
        if (memberRepository.existsByTaxId(member.getTaxId())) {
            throw new IllegalArgumentException("Member with Tax ID " + member.getTaxId() + " already exists.");
        }
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + id));
    }

    @Transactional
    public Member updateMember(Long id, Member updates) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + id));

        if (updates.getName() != null) member.setName(updates.getName());
        if (updates.getEmail() != null) member.setEmail(updates.getEmail());
        if (updates.getPhoneNumber() != null) member.setPhoneNumber(updates.getPhoneNumber());
        if (updates.getAddress() != null) member.setAddress(updates.getAddress());
        if (updates.getBirthDate() != null) member.setBirthDate(updates.getBirthDate());
        if (updates.getSex() != null) member.setSex(updates.getSex());

        return memberRepository.save(member);
    }

    @Transactional
    public boolean deleteMember(Long id) {
        if (!memberRepository.existsById(id)) {
            return false;
        }
        memberRepository.deleteById(id);
        return true;
    }
}
