package com.demo.gym.member;

import com.demo.gym.gym.Gym;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "gym_members")
@Getter
@Setter
@NoArgsConstructor
public class GymMember {
    @EmbeddedId
    private GymMemberId id;
    @ManyToOne
    @MapsId("gymId")
    @JoinColumn(name = "gym_id")
    private Gym gym;
    @ManyToOne
    @MapsId("memberId")
    @JoinColumn(name = "member_id")
    private Member member;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;
    @Column(name = "registered_at")
    private LocalDateTime registeredAt;
    // Nested embeddable ID class
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class GymMemberId implements Serializable {
        private Long gymId;
        private Long memberId;
    }
}