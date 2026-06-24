package com.demo.gym.acceptance.records;

public record GymRecord(
        String name,
        String taxId,
        String email,
        String phoneNumber,
        String websiteUrl,
        Integer capacity,
        String country,
        String city,
        String street,
        String zipCode
) {
}
