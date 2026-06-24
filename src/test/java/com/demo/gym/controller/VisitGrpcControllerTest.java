package com.demo.gym.controller;
import com.demo.gym.visit.VisitGrpcController;
import com.demo.gym.member.MemberGrpcController;
import com.demo.gym.gym.GymGrpcController;
import com.demo.gym.billing.BillGrpcController;

import static org.assertj.core.api.Assertions.assertThat;

import com.demo.gym.acceptance.records.GymRecord;
import com.demo.gym.acceptance.records.MemberRecord;
import com.demo.gym.controller.OutstandingBalanceResponse;
import com.demo.gym.util.GrpcTestSupport;
import com.demo.gym.util.InMemoryRecordLoader;
import com.demo.gym.util.TestEntityFactory;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VisitGrpcControllerTest {

    private static final InMemoryRecordLoader RECORDS = new InMemoryRecordLoader();

    @Autowired
    private VisitGrpcController visitGrpcController;

    @Autowired
    private MemberGrpcController memberGrpcController;

    @Autowired
    private GymGrpcController gymGrpcController;

    @Autowired
    private BillGrpcController billGrpcController;

    @Test
    void shouldCreateAndCancelVisit() {
        final long memberId = createMemberAndReturnId();
        final long gymId = createGymAndReturnId();

        // Create Visit without Registration fails
        CreateVisitRequest createVisitRequest = TestEntityFactory.createVisitRequest(gymId, memberId);
        TestStreamObserver<VisitResponse> observerVisitFail = new TestStreamObserver<>();
        visitGrpcController.createVisit(createVisitRequest, observerVisitFail);

        assertThat(observerVisitFail.getError()).isNotNull();
        assertThat(observerVisitFail.getError().getMessage())
                .contains("Member must be registered at gym");

        registerMemberAtGym(gymId, memberId);

        // Create Visit
        TestStreamObserver<VisitResponse> observerVisit = new TestStreamObserver<>();
        visitGrpcController.createVisit(createVisitRequest, observerVisit);

        assertThat(observerVisit.getError()).isNull();
        assertThat(observerVisit.getResponse()).isNotNull();
        final long stayId = observerVisit.getResponse().getId();
        assertThat(stayId).isGreaterThan(0);
        assertThat(observerVisit.getResponse().getGymId()).isEqualTo(gymId);
        assertThat(observerVisit.getResponse().getMemberId()).isEqualTo(memberId);
        assertThat(observerVisit.getResponse().getZone()).isEqualTo("101");
        assertThat(observerVisit.getResponse().getStationCode()).isEqualTo("A");
        assertThat(observerVisit.getResponse().getDailyRate()).isEqualTo(150.0);
        assertThat(observerVisit.getResponse().getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_IN);

        // Create the same Visit when it's Active should fail
        TestStreamObserver<VisitResponse> observerSameVisit = new TestStreamObserver<>();
        visitGrpcController.createVisit(createVisitRequest, observerSameVisit);

        assertThat(observerSameVisit.getError()).isNotNull();
        assertThat(observerSameVisit.getError().getMessage())
                .contains("Member already has a checked-in visit at this gym");

        // Cancel Visit
        CancelVisitRequest cancelVisitRequest = CancelVisitRequest.newBuilder()
                .setId(stayId)
                .setReason("Test cancellation")
                .build();
        TestStreamObserver<VisitResponse> observerCancel = new TestStreamObserver<>();
        visitGrpcController.cancelVisit(cancelVisitRequest, observerCancel);

        assertThat(observerCancel.getError()).isNull();
        assertThat(observerCancel.getResponse()).isNotNull();
        assertThat(observerCancel.getResponse().getStatus()).isEqualTo(VisitGrpcStatus.CANCELLED);

        CheckOutVisitRequest dischargeCancelledRequest = CheckOutVisitRequest.newBuilder()
                .setId(stayId)
                .build();
        CheckOutVisitResponse dischargedCancelled = GrpcTestSupport.checkOutVisit(visitGrpcController, dischargeCancelledRequest);
        assertThat(dischargedCancelled.getVisit().getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_OUT);

        OutstandingBalanceResponse outstanding = GrpcTestSupport.getOutstandingBalance(billGrpcController, memberId, gymId);
        assertThat(outstanding.getOutstandingAmount()).isEqualTo(0.0);

        BillResponse billResponse = getBill(dischargedCancelled.getBillId());
        assertThat(billResponse.getStatus()).isEqualTo(BillGrpcStatus.STATUS_CLOSED);
        assertThat(billResponse.getTotalAmount()).isEqualTo(0.0);
    }

    @Test
    void shouldCreateAndCompleteVisit() {
        final long memberId = createMemberAndReturnId();
        final long gymId = createGymAndReturnId();
        registerMemberAtGym(gymId, memberId);
        final long stayId = createVisit(gymId, memberId).getId();

        // Complete Visit
        CompleteVisitRequest completeVisitRequest = CompleteVisitRequest.newBuilder()
                .setId(stayId)
                .build();
        VisitResponse completeResponse = GrpcTestSupport.completeVisit(visitGrpcController, completeVisitRequest);
        assertThat(completeResponse.getStatus()).isEqualTo(VisitGrpcStatus.COMPLETED);

        // Get Member Visits
        VisitListResponse stayListResponse = GrpcTestSupport.getMemberVisits(visitGrpcController, memberId);
        assertThat(stayListResponse.getVisitsList()).hasSize(1);
        assertThat(stayListResponse.getVisitsList().get(0).getMemberId()).isEqualTo(memberId);
    }

    @Test
    void shouldCreateAndCheckOutVisit() {
        final long memberId = createMemberAndReturnId();
        final long gymId = createGymAndReturnId();
        registerMemberAtGym(gymId, memberId);
        Instant checkIn = Instant.now().minus(7, ChronoUnit.DAYS);
        final double dailyRate = 150.0;
        final double expectedTotal = dailyRate * 7; // 1050.0
        VisitResponse createdVisit = GrpcTestSupport.createVisit(
                visitGrpcController,
                buildVisitRequest(gymId, memberId, checkIn, dailyRate)
        );

        // Check Bill Created
        assertThat(createdVisit.getBillId()).isGreaterThan(0);
        final long billId = createdVisit.getBillId();

        BillResponse billResponse = getBill(billId);
        assertThat(billResponse.getId()).isEqualTo(billId);
        assertThat(billResponse.getStatus()).isEqualTo(BillGrpcStatus.STATUS_OPEN);

        OutstandingBalanceResponse outstandingBeforeDischarge =
                GrpcTestSupport.getOutstandingBalance(billGrpcController, memberId, gymId);
        assertThat(outstandingBeforeDischarge.getOutstandingAmount()).isEqualTo(expectedTotal);

        // Discharge Visit
        final long stayId = createdVisit.getId();
        CheckOutVisitRequest checkOutVisitRequest = CheckOutVisitRequest.newBuilder()
                .setId(stayId)
                .build();
        CheckOutVisitResponse dischargedResponse = GrpcTestSupport.checkOutVisit(visitGrpcController, checkOutVisitRequest);
        assertThat(dischargedResponse.getBillId()).isEqualTo(billId);
        assertThat(dischargedResponse.getVisit().getBillId()).isEqualTo(billId);
        assertThat(dischargedResponse.getVisit().getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_OUT);

        billResponse = getBill(billId);
        assertThat(billResponse.getId()).isEqualTo(billId);
        assertThat(billResponse.getStatus()).isEqualTo(BillGrpcStatus.STATUS_CLOSED);
        assertThat(billResponse.getTotalAmount()).isEqualTo(expectedTotal);
        assertThat(billResponse.getGymId()).isEqualTo(gymId);

        // Get Member Visits
        VisitListResponse stayListResponse = GrpcTestSupport.getMemberVisits(visitGrpcController, memberId);
        assertThat(stayListResponse.getVisitsList()).hasSize(1);
        assertThat(stayListResponse.getVisitsList().get(0).getMemberId()).isEqualTo(memberId);
    }

    @Test
    void shouldReturnVisitSummaryQuarter() {
        final long memberId = createMemberAndReturnId();
        final long gymId = createGymAndReturnId();
        registerMemberAtGym(gymId, memberId);

        // Visit #1: 2026-01-10 -> 2026-02-10 => 31 days * 100 = 3100
        VisitResponse stay1 = GrpcTestSupport.createVisit(visitGrpcController,
                buildVisitRequest(gymId, memberId, Instant.parse("2026-01-10T00:00:00Z"), 100.0));
        GrpcTestSupport.completeVisit(visitGrpcController,
                CompleteVisitRequest.newBuilder()
                        .setId(stay1.getId())
                        .setEndDate(toTimestamp(Instant.parse("2026-02-10T00:00:00Z")))
                        .build());

        // Visit #2: 2026-03-01 -> 2025-06-21 => 20 days * 150 = 3000
        VisitResponse stay2 = GrpcTestSupport.createVisit(visitGrpcController,
                buildVisitRequest(gymId, memberId, Instant.parse("2026-03-01T00:00:00Z"), 150.0));
        GrpcTestSupport.checkOutVisit(visitGrpcController,
                CheckOutVisitRequest.newBuilder()
                        .setId(stay2.getId())
                        .setCheckOut(toTimestamp(Instant.parse("2026-03-21T00:00:00Z")))
                        .build());

        GetVisitSummaryQuarterResponse summary = GrpcTestSupport.getVisitSummaryQuarter(
                visitGrpcController,
                GetVisitSummaryQuarterRequest.newBuilder()
                        .setMemberId(memberId)
                        .setGymId(gymId)
                        .setYear(2026)
                        .setQuarter(1)
                        .build());

        assertThat(summary.getMemberId()).isEqualTo(memberId);
        assertThat(summary.getGymId()).isEqualTo(gymId);
        assertThat(summary.getTotalDays()).isEqualTo(51);
        assertThat(summary.getBilledAmount()).isEqualTo(6100.0);
        assertThat(summary.getCurrency()).isEqualTo("EUR");
    }

    private GymRecord fixtureGym() {
        return RECORDS.loadGyms().get("St Mary Gym");
    }

    private MemberRecord fixtureMember() {
        return RECORDS.loadMembers().get("Mary");
    }

    private CreateMemberRequest createMemberRequestFromFixture() {
        return GrpcTestSupport.toCreateMemberRequest(fixtureMember());
    }

    private CreateGymRequest createGymRequestFromFixture() {
        return GrpcTestSupport.toCreateGymRequest(fixtureGym());
    }

    private long createMemberAndReturnId() {
        return GrpcTestSupport.createMember(memberGrpcController, createMemberRequestFromFixture()).getId();
    }

    private long createGymAndReturnId() {
        return GrpcTestSupport.createGym(gymGrpcController, createGymRequestFromFixture()).getId();
    }

    private void registerMemberAtGym(long gymId, long memberId) {
        GrpcTestSupport.registerMember(gymGrpcController, gymId, memberId);
    }

    private VisitResponse createVisit(long gymId, long memberId) {
        CreateVisitRequest createVisitRequest = TestEntityFactory.createVisitRequest(gymId, memberId);
        return GrpcTestSupport.createVisit(visitGrpcController, createVisitRequest);
    }

    private BillResponse getBill(long billId) {
        return GrpcTestSupport.getBill(billGrpcController, billId);
    }

    private CreateVisitRequest buildVisitRequest(long gymId, long memberId, Instant admission, double dailyRate) {
        return CreateVisitRequest.newBuilder()
                .setGymId(gymId)
                .setMemberId(memberId)
                .setCheckIn(toTimestamp(admission))
                .setZone("101")
                .setStationCode("A")
                .setDailyRate(dailyRate)
                .build();
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}