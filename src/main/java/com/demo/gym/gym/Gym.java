package com.demo.gym.gym;

import com.demo.gym.common.Address;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gyms")
@Getter
@Setter
@NoArgsConstructor
public class Gym {

    @Id
    @Setter(AccessLevel.PROTECTED)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gym_seq_gen")
    @SequenceGenerator(name = "gym_seq_gen", sequenceName = "gym_seq", allocationSize = 1)
    private Long id;

    @Column(name = "tax_id", nullable = false, unique = true)
    private String taxId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private GymType type;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String websiteUrl;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private Integer capacity;

    @Embedded
    @Column(nullable = false)
    private Address address;
}