package com.demo.gym.common;
import com.demo.gym.controller.MemberResponse;
import com.demo.gym.controller.AddressMessage;
import com.demo.gym.controller.Sex;
import com.demo.gym.controller.MemberResponse;
import com.demo.gym.controller.AddressMessage;
import com.demo.gym.controller.Sex;

import com.demo.gym.member.Member;
import com.demo.gym.member.BiologicalSex;
import com.google.protobuf.Timestamp;
import com.google.type.Date;

import java.time.Instant;

public class Utils {

    public static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public static MemberResponse buildMemberResponse(Member member) {
        MemberResponse.Builder builder = MemberResponse.newBuilder()
                .setId(member.getId())
                .setName(member.getName())
                .setTaxId(member.getTaxId())
                .setEmail(member.getEmail() != null ? member.getEmail() : "")
                .setPhoneNumber(member.getPhoneNumber());

        if (member.getBirthDate() != null) {
            builder.setBirthDate(Date.newBuilder()
                    .setYear(member.getBirthDate().getYear())
                    .setMonth(member.getBirthDate().getMonthValue())
                    .setDay(member.getBirthDate().getDayOfMonth())
                    .build());
        }

        if (member.getAddress() != null) {
            builder.setAddress(AddressMessage.newBuilder()
                    .setCountry(member.getAddress().country())
                    .setCity(member.getAddress().city())
                    .setStreet(member.getAddress().street())
                    .setZipCode(member.getAddress().zipCode())
                    .build());
        }

        if (member.getSex() != null) {
            builder.setSex(BiologicalSex.mapSexToProto(member.getSex()));
        }

        return builder.build();
    }
}
