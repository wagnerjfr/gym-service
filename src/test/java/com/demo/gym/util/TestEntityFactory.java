package com.demo.gym.util;

import com.demo.gym.controller.AddressMessage;
import com.demo.gym.controller.CreateGymRequest;
import com.demo.gym.controller.CreateMemberRequest;
import com.demo.gym.controller.CreateVisitRequest;
import com.demo.gym.controller.Sex;
import com.demo.gym.common.Address;
import com.demo.gym.gym.Gym;
import com.demo.gym.member.Member;
import com.demo.gym.visit.Visit;
import com.demo.gym.member.BiologicalSex;
import com.demo.gym.visit.VisitStatus;
import com.google.type.Date;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class TestEntityFactory {

    public static CreateGymRequest createGymRequest() {
        AddressMessage address = AddressMessage.newBuilder()
                .setCountry("Germany")
                .setCity("Munich")
                .setStreet("street")
                .setZipCode("12345")
                .build();

        return CreateGymRequest.newBuilder()
                .setName("Test Gym")
                .setTaxId("12345")
                .setEmail("gym@com")
                .setPhoneNumber("12345")
                .setWebsiteUrl("www.testgym.com")
                .setCapacity(100)
                .setAddress(address)
                .build();
    }

    public static CreateMemberRequest createMemberRequest() {
        AddressMessage address = AddressMessage.newBuilder()
                .setCountry("Germany")
                .setCity("Berlin")
                .setStreet("Street 123")
                .setZipCode("12345")
                .build();

        Date birthDate = Date.newBuilder()
                .setYear(1990)
                .setMonth(5)
                .setDay(15)
                .build();

        return CreateMemberRequest.newBuilder()
                .setName("John Lucas")
                .setTaxId("123456")
                .setEmail("john.lucas@email.com")
                .setPhoneNumber("987-654-3210")
                .setAddress(address)
                .setBirthDate(birthDate)
                .setSex(Sex.SEX_MALE)
                .build();
    }

    public static CreateVisitRequest createVisitRequest(long gymId, long memberId) {
        return CreateVisitRequest.newBuilder()
                .setGymId(gymId)
                .setMemberId(memberId)
                .setZone("101")
                .setStationCode("A")
                .setDailyRate(150.0)
                .build();
    }

    public static Gym createTestGym() {
        return createTestGym("TAX123", "Test Gym");
    }

    public static Gym createTestGym(Long id) {
        return createTestGym(id, "TAX123", "Test Gym");
    }

    public static Gym createTestGym(String taxId, String name) {
        Gym h = new Gym();
        h.setName(name);
        h.setTaxId(taxId);
        h.setEmail("test@gym.com");
        h.setPhoneNumber("123-456");
        h.setWebsiteUrl("www.test.com");
        h.setCapacity(100);
        h.setAddress(createTestAddress());
        return h;
    }

    public static Gym createTestGym(Long id, String taxId, String name) {
        Gym h = createTestGym(taxId, name);
        ReflectionTestUtils.setField(h, "id", id);
        return h;
    }

    public static Member createTestMember() {
        return createTestMember("PAT123", "Test Member");
    }

    public static Member createTestMember(Long id) {
        return createTestMember(id, "PAT123", "Test Member");
    }

    public static Member createTestMember(String taxId, String name) {
        Member p = new Member();
        p.setName(name);
        p.setTaxId(taxId);
        p.setEmail("test@member.com");
        p.setPhoneNumber("555-1234");
        p.setBirthDate(LocalDate.of(1990, 1, 15));
        p.setSex(BiologicalSex.MALE);
        p.setAddress(createTestAddress());
        return p;
    }

    public static Member createTestMember(Long id, String taxId, String name) {
        Member p = createTestMember(taxId, name);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    public static Address createTestAddress() {
        return new Address("Germany", "Munich", "123 Main St", "02101");
    }

    public static Visit createTestVisit(Gym gym, Member member) {
        return createTestVisit(gym, member, VisitStatus.CHECKED_IN);
    }

    public static Visit createTestVisit(Gym gym, Member member, VisitStatus status) {
        Visit visit = new Visit();
        visit.setGym(gym);
        visit.setMember(member);
        visit.setCheckIn(Instant.now());
        visit.setStatus(status);
        visit.setDailyRate(new BigDecimal("100.00"));
        visit.setCreatedAt(Instant.now());
        return visit;
    }

    public static Visit createTestVisit(Long id, Gym gym, Member member) {
        Visit visit = createTestVisit(gym, member);
        ReflectionTestUtils.setField(visit, "id", id);
        return visit;
    }
}
