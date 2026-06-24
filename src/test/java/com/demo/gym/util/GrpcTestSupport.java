package com.demo.gym.util;

import com.demo.gym.acceptance.records.GymRecord;
import com.demo.gym.acceptance.records.MemberRecord;
import com.demo.gym.controller.AddressMessage;
import com.demo.gym.billing.BillGrpcController;
import com.demo.gym.controller.BillResponse;
import com.demo.gym.controller.CancelVisitRequest;
import com.demo.gym.controller.CompleteVisitRequest;
import com.demo.gym.controller.CreateGymRequest;
import com.demo.gym.controller.CreateMemberRequest;
import com.demo.gym.controller.CreateVisitRequest;
import com.demo.gym.controller.CheckOutVisitRequest;
import com.demo.gym.controller.CheckOutVisitResponse;
import com.demo.gym.controller.GetBillRequest;
import com.demo.gym.controller.GetGymRequest;
import com.demo.gym.controller.GetMemberRequest;
import com.demo.gym.controller.GetMemberBillsRequest;
import com.demo.gym.controller.GetMemberVisitsRequest;
import com.demo.gym.gym.GymGrpcController;
import com.demo.gym.controller.GymListResponse;
import com.demo.gym.controller.GymResponse;
import com.demo.gym.controller.OutstandingBalanceRequest;
import com.demo.gym.controller.OutstandingBalanceResponse;
import com.demo.gym.member.MemberGrpcController;
import com.demo.gym.controller.MemberBillsResponse;
import com.demo.gym.controller.MemberListResponse;
import com.demo.gym.controller.MemberResponse;
import com.demo.gym.controller.RegisterMemberRequest;
import com.demo.gym.controller.MembershipResponse;
import com.demo.gym.controller.Sex;
import com.demo.gym.visit.VisitGrpcController;
import com.demo.gym.controller.VisitListResponse;
import com.demo.gym.controller.VisitResponse;
import com.demo.gym.controller.GetVisitSummaryQuarterRequest;
import com.demo.gym.controller.GetVisitSummaryQuarterResponse;
import com.demo.gym.controller.TestStreamObserver;
import com.google.type.Date;

import static org.assertj.core.api.Assertions.assertThat;

public final class GrpcTestSupport {

    private GrpcTestSupport() {
    }

    public static CreateGymRequest toCreateGymRequest(GymRecord record) {
        return CreateGymRequest.newBuilder()
                .setName(record.name())
                .setTaxId(record.taxId())
                .setEmail(record.email())
                .setPhoneNumber(record.phoneNumber())
                .setWebsiteUrl(record.websiteUrl())
                .setCapacity(record.capacity())
                .setAddress(AddressMessage.newBuilder()
                        .setCountry(record.country())
                        .setCity(record.city())
                        .setStreet(record.street())
                        .setZipCode(record.zipCode())
                        .build())
                .build();
    }

    public static CreateMemberRequest toCreateMemberRequest(MemberRecord record) {
        return CreateMemberRequest.newBuilder()
                .setName(record.name())
                .setTaxId(record.taxId())
                .setPhoneNumber(record.phoneNumber())
                .setBirthDate(Date.newBuilder()
                        .setYear(record.birthYear())
                        .setMonth(record.birthMonth())
                        .setDay(record.birthDay())
                        .build())
                .setSex(mapSex(record.sex()))
                .setAddress(AddressMessage.newBuilder()
                        .setCountry(record.country())
                        .setCity(record.city())
                        .setStreet(record.street())
                        .setZipCode(record.zipCode())
                        .build())
                .build();
    }

    public static GymResponse createGym(GymGrpcController controller, CreateGymRequest request) {
        TestStreamObserver<GymResponse> observer = new TestStreamObserver<>();
        controller.createGym(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static MemberResponse createMember(MemberGrpcController controller, CreateMemberRequest request) {
        TestStreamObserver<MemberResponse> observer = new TestStreamObserver<>();
        controller.createMember(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static MembershipResponse registerMember(GymGrpcController controller, long gymId, long memberId) {
        RegisterMemberRequest request = RegisterMemberRequest.newBuilder()
                .setGymId(gymId)
                .setMemberId(memberId)
                .build();
        TestStreamObserver<MembershipResponse> observer = new TestStreamObserver<>();
        controller.registerMember(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static VisitResponse createVisit(VisitGrpcController controller, CreateVisitRequest request) {
        TestStreamObserver<VisitResponse> observer = new TestStreamObserver<>();
        controller.createVisit(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static Throwable createVisitError(VisitGrpcController controller, CreateVisitRequest request) {
        TestStreamObserver<VisitResponse> observer = new TestStreamObserver<>();
        controller.createVisit(request, observer);
        assertThat(observer.getError()).isNotNull();
        return observer.getError();
    }

    public static VisitResponse completeVisit(VisitGrpcController controller, CompleteVisitRequest request) {
        TestStreamObserver<VisitResponse> observer = new TestStreamObserver<>();
        controller.completeVisit(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static CheckOutVisitResponse checkOutVisit(VisitGrpcController controller, CheckOutVisitRequest request) {
        TestStreamObserver<CheckOutVisitResponse> observer = new TestStreamObserver<>();
        controller.checkOutVisit(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static VisitResponse cancelVisit(VisitGrpcController controller, CancelVisitRequest request) {
        TestStreamObserver<VisitResponse> observer = new TestStreamObserver<>();
        controller.cancelVisit(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static BillResponse getBill(BillGrpcController controller, long billId) {
        GetBillRequest request = GetBillRequest.newBuilder().setId(billId).build();
        TestStreamObserver<BillResponse> observer = new TestStreamObserver<>();
        controller.getBill(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static MemberBillsResponse getMemberBills(BillGrpcController controller, long memberId) {
        GetMemberBillsRequest request = GetMemberBillsRequest.newBuilder().setMemberId(memberId).build();
        TestStreamObserver<MemberBillsResponse> observer = new TestStreamObserver<>();
        controller.getMemberBills(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static OutstandingBalanceResponse getOutstandingBalance(BillGrpcController controller, long memberId, long gymId) {
        OutstandingBalanceRequest request = OutstandingBalanceRequest.newBuilder()
                .setMemberId(memberId)
                .setGymId(gymId)
                .build();
        TestStreamObserver<OutstandingBalanceResponse> observer = new TestStreamObserver<>();
        controller.getOutstandingBalance(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static GymListResponse getMemberGyms(GymGrpcController controller, long memberId) {
        GetMemberRequest request = GetMemberRequest.newBuilder().setId(memberId).build();
        TestStreamObserver<GymListResponse> observer = new TestStreamObserver<>();
        controller.getMemberGyms(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static MemberListResponse getGymMembers(GymGrpcController controller, long gymId) {
        GetGymRequest request = GetGymRequest.newBuilder().setId(gymId).build();
        TestStreamObserver<MemberListResponse> observer = new TestStreamObserver<>();
        controller.getGymMembers(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static VisitListResponse getMemberVisits(VisitGrpcController controller, long memberId) {
        GetMemberVisitsRequest request = GetMemberVisitsRequest.newBuilder().setMemberId(memberId).build();
        TestStreamObserver<VisitListResponse> observer = new TestStreamObserver<>();
        controller.getMemberVisits(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static GetVisitSummaryQuarterResponse getVisitSummaryQuarter(VisitGrpcController controller,
                                                                      GetVisitSummaryQuarterRequest request) {
        TestStreamObserver<GetVisitSummaryQuarterResponse> observer = new TestStreamObserver<>();
        controller.getVisitSummaryQuarter(request, observer);
        assertThat(observer.getError()).isNull();
        assertThat(observer.getResponse()).isNotNull();
        return observer.getResponse();
    }

    public static Sex mapSex(String sex) {
        if (sex == null) {
            return Sex.SEX_UNSPECIFIED;
        }
        return switch (sex.trim().toUpperCase()) {
            case "M" -> Sex.SEX_MALE;
            case "F" -> Sex.SEX_FEMALE;
            default -> Sex.SEX_UNSPECIFIED;
        };
    }
}