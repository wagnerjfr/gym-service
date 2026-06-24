package com.demo.gym.acceptance.records;

public record MemberRecord(
        String name,
        String taxId,
        String phoneNumber,
        Integer birthYear,
        Integer birthMonth,
        Integer birthDay,
        String sex,
        String country,
        String city,
        String street,
        String zipCode
) {
}
