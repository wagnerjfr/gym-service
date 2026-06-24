package com.demo.gym.acceptance;

import com.demo.gym.acceptance.records.GymRecord;
import com.demo.gym.acceptance.records.MemberRecord;
import com.demo.gym.billing.BillGrpcController;
import com.demo.gym.controller.BillGrpcStatus;
import com.demo.gym.controller.BillResponse;
import com.demo.gym.controller.CancelVisitRequest;
import com.demo.gym.controller.CompleteVisitRequest;
import com.demo.gym.controller.CreateVisitRequest;
import com.demo.gym.controller.CheckOutVisitRequest;
import com.demo.gym.controller.CheckOutVisitResponse;
import com.demo.gym.gym.GymGrpcController;
import com.demo.gym.controller.GymListResponse;
import com.demo.gym.controller.OutstandingBalanceResponse;
import com.demo.gym.controller.MemberBillsResponse;
import com.demo.gym.member.MemberGrpcController;
import com.demo.gym.controller.MemberListResponse;
import com.demo.gym.controller.MemberResponse;
import com.demo.gym.visit.VisitGrpcController;
import com.demo.gym.controller.VisitGrpcStatus;
import com.demo.gym.controller.VisitResponse;
import com.demo.gym.controller.GetVisitSummaryQuarterRequest;
import com.demo.gym.controller.GetVisitSummaryQuarterResponse;
import com.demo.gym.util.GrpcTestSupport;
import com.demo.gym.util.InMemoryRecordLoader;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
public class AcceptanceTest {

    @Autowired
    private GymGrpcController gymGrpcController;

    @Autowired
    private MemberGrpcController memberGrpcController;

    @Autowired
    private VisitGrpcController visitGrpcController;

    @Autowired
    private BillGrpcController billGrpcController;

    private final InMemoryRecordLoader inMemoryRecordLoader = new InMemoryRecordLoader();

    @Test
    /*
     * End-to-end scenario summary:
     * 1) Load and persist fixture gyms/members, then register members in both gyms.
     * 2) Run first St Mary sequence for Mary:
     *    - create visit and validate outstanding balance while bill is open,
     *    - reject a second active visit,
     *    - complete first visit, create second visit, assert exact overdue before closure,
     *    - check-out and close the first St Mary bill.
     * 3) Run St Paul sequence with multiple visits and final check-out to close St Paul bill.
     * 4) Run final St Mary sequence (ACTIVE -> CANCELLED -> CHECKED_OUT), producing a zero-value bill.
     * 5) Validate final billing outcome for Mary: 3 bills with totals [0.0, 2500.0, 4300.0].
     */
    void shouldMaryCreateMultipleVisitsInTwoDifferentGyms() {
        Map<String, GymRecord> gyms = inMemoryRecordLoader.loadGyms();
        Map<String, MemberRecord> members = inMemoryRecordLoader.loadMembers();
        Map<String, Long> gymIdsByName = persistGyms(gyms);
        Map<String, Long> memberIdsByName = persistMembers(members);

        final String firstGym = "St Mary Gym";
        final String secondGym = "St Paul Gym";
        final String personName = "Mary";

        // ** Mary is registered in all gyms **
        registerMemberGym(gymIdsByName.get(firstGym), memberIdsByName.get("Alice"));
        registerMemberGym(gymIdsByName.get(firstGym), memberIdsByName.get("Tom"));
        registerMemberGym(gymIdsByName.get(firstGym), memberIdsByName.get("Kate"));
        registerMemberGym(gymIdsByName.get(firstGym), memberIdsByName.get("Mary"));

        registerMemberGym(gymIdsByName.get(secondGym), memberIdsByName.get("John"));
        registerMemberGym(gymIdsByName.get(secondGym), memberIdsByName.get("Bob"));
        registerMemberGym(gymIdsByName.get(secondGym), memberIdsByName.get("Mary"));


        // ** Mary is registered in all gyms **
        assertThat(getMemberGyms(memberIdsByName.get(personName)).getGymsCount()).isEqualTo(2);

        MemberListResponse stMaryMembers = getGymMembers(gymIdsByName.get(firstGym));
        assertThat(stMaryMembers.getMembersCount()).isEqualTo(4);
        assertThat(stMaryMembers.getMembersList())
                .extracting(MemberResponse::getName)
                .containsExactlyInAnyOrder("Alice", "Tom", "Kate", "Mary");

        // Mary stays
        Long stMaryGymId = gymIdsByName.get(firstGym);
        Long maryMemberId = memberIdsByName.get(personName);

        // Fixed deterministic dates (Q1 2026)
        Instant stMaryVisit1Admission = Instant.parse("2026-01-01T00:00:00Z");
        Instant stMaryVisit1End = Instant.parse("2026-01-11T00:00:00Z");
        Instant stMaryVisit2Discharge = Instant.parse("2026-01-21T00:00:00Z");

        Instant stPaulVisit1Admission = Instant.parse("2026-02-01T00:00:00Z");
        Instant stPaulVisit1End = Instant.parse("2026-02-11T00:00:00Z");
        Instant stPaulVisit2Admission = Instant.parse("2026-02-18T00:00:00Z");
        Instant stPaulVisit2End = Instant.parse("2026-02-28T00:00:00Z");
        Instant stPaulVisit3Admission = Instant.parse("2026-03-01T00:00:00Z");
        Instant stPaulVisit3Discharge = Instant.parse("2026-03-11T00:00:00Z");

        Instant stMaryCancelledAdmission = Instant.parse("2026-03-15T00:00:00Z");
        Instant stMaryCancelledDischarge = Instant.parse("2026-03-16T00:00:00Z");

        CreateVisitRequest createVisitRequest = buildCreateVisitRequest(
                stMaryGymId,
                maryMemberId,
                stMaryVisit1Admission,
                100);

        VisitResponse visitResponse = createVisit(createVisitRequest);
        assertThat(visitResponse.getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_IN);
        assertThat(visitResponse.getBillId()).isGreaterThan(0);
        long visitId = visitResponse.getId();
        long billId = visitResponse.getBillId();

        OutstandingBalanceResponse outstandingBalance = GrpcTestSupport
                .getOutstandingBalance(billGrpcController, maryMemberId, stMaryGymId);
        assertThat(outstandingBalance.getMemberId()).isEqualTo(maryMemberId);
        assertThat(outstandingBalance.getGymId()).isEqualTo(stMaryGymId);
        assertThat(outstandingBalance.getCurrency()).isEqualTo("EUR");
        assertThat(outstandingBalance.getOutstandingAmount()).isGreaterThan(0.0);

        Throwable t = createVisitError(createVisitRequest);
        assertThat(t)
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("FAILED_PRECONDITION")
                .hasMessageContaining("Member already has a checked-in visit at this gym");

        BillResponse billResponse = getBill(billId);
        assertThat(billResponse.getStatus()).isEqualTo(BillGrpcStatus.STATUS_OPEN);
        assertThat(billResponse.getTotalAmount()).isEqualTo(0);

        CompleteVisitRequest completeVisitRequest = buildCompleteVisitRequest(visitId, stMaryVisit1End);
        visitResponse = completeVisit(completeVisitRequest);
        assertThat(visitResponse.getStatus()).isEqualTo(VisitGrpcStatus.COMPLETED);

        createVisitRequest = buildCreateVisitRequest(
                stMaryGymId,
                maryMemberId,
                stMaryVisit1End,
                150);
        visitResponse = createVisit(createVisitRequest);
        assertThat(visitResponse.getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_IN);
        assertThat(visitResponse.getBillId()).isEqualTo(billId);

        CheckOutVisitRequest checkOutVisitRequest = buildCheckOutVisitRequest(visitResponse.getId(), stMaryVisit2Discharge);
        CheckOutVisitResponse checkOutVisitResponse = checkOutVisit(checkOutVisitRequest);
        
        assertThat(checkOutVisitResponse.getVisit().getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_OUT);
        assertThat(checkOutVisitResponse.getBillId()).isEqualTo(billId);

        billResponse = getBill(billId);
        assertThat(billResponse.getStatus()).isEqualTo(BillGrpcStatus.STATUS_CLOSED);
        assertThat(billResponse.getTotalAmount()).isGreaterThan(0);
        assertThat(billResponse.getGymId()).isEqualTo(stMaryGymId);

        // Mary also creates 2 completed stays in another gym

        Long stPaulGymId = gymIdsByName.get(secondGym);

        CreateVisitRequest stPaulVisitRequest1 = buildCreateVisitRequest(
                stPaulGymId,
                maryMemberId,
                stPaulVisit1Admission,
                130);
        VisitResponse stPaulVisit1 = createVisit(stPaulVisitRequest1);
        assertThat(stPaulVisit1.getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_IN);
        assertThat(stPaulVisit1.getId()).isGreaterThan(0);

        VisitResponse stPaulVisit1Completed = completeVisit(buildCompleteVisitRequest(
                stPaulVisit1.getId(),
                stPaulVisit1End));
        assertThat(stPaulVisit1Completed.getStatus()).isEqualTo(VisitGrpcStatus.COMPLETED);

        CreateVisitRequest stPaulVisitRequest2 = buildCreateVisitRequest(
                stPaulGymId,
                maryMemberId,
                stPaulVisit2Admission,
                140);
        VisitResponse stPaulVisit2 = createVisit(stPaulVisitRequest2);
        assertThat(stPaulVisit2.getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_IN);
        assertThat(stPaulVisit2.getId()).isGreaterThan(0);

        VisitResponse stPaulVisit2Completed = completeVisit(buildCompleteVisitRequest(
                stPaulVisit2.getId(),
                stPaulVisit2End));
        assertThat(stPaulVisit2Completed.getStatus()).isEqualTo(VisitGrpcStatus.COMPLETED);

        // Create another visit and check out
        CreateVisitRequest stPaulVisitRequest3 = buildCreateVisitRequest(
                stPaulGymId,
                maryMemberId,
                stPaulVisit3Admission,
                160);
        VisitResponse stPaulVisit3 = createVisit(stPaulVisitRequest3);
        assertThat(stPaulVisit3.getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_IN);
        assertThat(stPaulVisit3.getId()).isGreaterThan(0);
        assertThat(stPaulVisit3.getBillId()).isGreaterThan(0);

        CheckOutVisitResponse stPaulDischargeResponse = checkOutVisit(buildCheckOutVisitRequest(
                stPaulVisit3.getId(),
                stPaulVisit3Discharge));
        assertThat(stPaulDischargeResponse.getVisit().getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_OUT);
        assertThat(stPaulDischargeResponse.getVisit().getId()).isEqualTo(stPaulVisit3.getId());
        assertThat(stPaulDischargeResponse.getBillId()).isEqualTo(stPaulVisit3.getBillId());

        // Final St Mary flow: create ACTIVE -> CANCEL -> DISCHARGE
        CreateVisitRequest stMaryCancelledFlow = buildCreateVisitRequest(
                stMaryGymId,
                maryMemberId,
                stMaryCancelledAdmission,
                200);
        VisitResponse stMaryLastVisit = createVisit(stMaryCancelledFlow);
        assertThat(stMaryLastVisit.getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_IN);

        VisitResponse stMaryCancelled = cancelVisit(CancelVisitRequest.newBuilder()
                .setId(stMaryLastVisit.getId())
                .setReason("Administrative cancellation")
                .build());
        assertThat(stMaryCancelled.getStatus()).isEqualTo(VisitGrpcStatus.CANCELLED);

        CheckOutVisitResponse stMaryCancelledDischarged = checkOutVisit(buildCheckOutVisitRequest(
                stMaryLastVisit.getId(),
                stMaryCancelledDischarge));
        assertThat(stMaryCancelledDischarged.getVisit().getStatus()).isEqualTo(VisitGrpcStatus.CHECKED_OUT);

        BillResponse zeroBill = getBill(stMaryCancelledDischarged.getBillId());
        assertThat(zeroBill.getGymId()).isEqualTo(stMaryGymId);
        assertThat(zeroBill.getStatus()).isEqualTo(BillGrpcStatus.STATUS_CLOSED);
        assertThat(zeroBill.getTotalAmount()).isEqualTo(0.0);

        // Billing expectation summary (days are computed like BillService: Duration.between(checkIn, checkOut).toDays())
        // St Mary bill:
        //   - stay1: 2026-01-01 -> 2026-01-11 = 10 days * 100 = 1000
        //   - stay2: 2026-01-11 -> 2026-01-21 = 10 days * 150 = 1500
        //   => expected St Mary total = 2500
        // St Paul bill:
        //   - stay1: 2026-02-01 -> 2026-02-11 = 10 days * 130 = 1300
        //   - stay2: 2026-02-18 -> 2026-02-28 = 10 days * 140 = 1400
        //   - stay3: 2026-03-01 -> 2026-03-11 = 10 days * 160 = 1600
        //   => expected St Paul total = 4300
        // New final St Mary cancelled+checked-out visit creates a 3rd bill with zero total.
        // Total expected for Mary's 3 bills = 6800.

        // Validate Mary has exactly 3 bills in bill service
        MemberBillsResponse maryBills = getMemberBills(maryMemberId);
        assertThat(maryBills.getBillsCount()).isEqualTo(3);

        double total = 0;
        for (BillResponse response : maryBills.getBillsList()) {
            total += response.getTotalAmount();
        }

        List<Double> amounts = maryBills.getBillsList().stream()
                .map(BillResponse::getTotalAmount)
                .sorted(Comparator.naturalOrder())
                .toList();
        assertThat(amounts).containsExactly(0.0, 2500.0, 4300.0);
        assertThat(total).isEqualTo(6800.0);

        // Summary across all gyms for Q1 2026.
        // gym_id = 0 means all gyms.
        GetVisitSummaryQuarterResponse allGymsSummary = GrpcTestSupport.getVisitSummaryQuarter(
                visitGrpcController,
                GetVisitSummaryQuarterRequest.newBuilder()
                        .setMemberId(maryMemberId)
                        .setGymId(0)
                        .setYear(2026)
                        .setQuarter(1)
                        .build()
        );
        assertSummaryQuarter(allGymsSummary, maryMemberId, 0, 50, 6800.0);

        // Summary across St Mary Gym for Q1 2026.
        GetVisitSummaryQuarterResponse stMaryGymsSummary = GrpcTestSupport.getVisitSummaryQuarter(
                visitGrpcController,
                GetVisitSummaryQuarterRequest.newBuilder()
                        .setMemberId(maryMemberId)
                        .setGymId(stMaryGymId)
                        .setYear(2026)
                        .setQuarter(1)
                        .build()
        );
        assertSummaryQuarter(stMaryGymsSummary, maryMemberId, stMaryGymId, 20, 2500.0);

        // Summary across all gyms for Q4 2025.
        // gym_id = 0 means all gyms.
        GetVisitSummaryQuarterResponse noDataGymsSummary = GrpcTestSupport.getVisitSummaryQuarter(
                visitGrpcController,
                GetVisitSummaryQuarterRequest.newBuilder()
                        .setMemberId(maryMemberId)
                        .setGymId(0)
                        .setYear(2025)
                        .setQuarter(4)
                        .build()
        );
        assertSummaryQuarter(noDataGymsSummary, maryMemberId, 0, 0, 0.0);
    }

    private void assertSummaryQuarter(GetVisitSummaryQuarterResponse summaryQuarterResponse, long memberId,
                                      long gymId, int days, double amount) {
        assertThat(summaryQuarterResponse.getMemberId()).isEqualTo(memberId);
        assertThat(summaryQuarterResponse.getGymId()).isEqualTo(gymId);
        assertThat(summaryQuarterResponse.getCurrency()).isEqualTo("EUR");
        assertThat(summaryQuarterResponse.getTotalDays()).isEqualTo(days);
        assertThat(summaryQuarterResponse.getBilledAmount()).isEqualTo(amount);
    }

    private void registerMemberGym(long gymId, long memberId) {
        GrpcTestSupport.registerMember(gymGrpcController, gymId, memberId);
    }

    private GymListResponse getMemberGyms(long memberId) {
        return GrpcTestSupport.getMemberGyms(gymGrpcController, memberId);
    }

    private MemberListResponse getGymMembers(long gymId) {
        return GrpcTestSupport.getGymMembers(gymGrpcController, gymId);
    }

    private VisitResponse createVisit(CreateVisitRequest request) {
        return GrpcTestSupport.createVisit(visitGrpcController, request);
    }

    private Throwable createVisitError(CreateVisitRequest request) {
        return GrpcTestSupport.createVisitError(visitGrpcController, request);
    }

    private VisitResponse completeVisit(CompleteVisitRequest request) {
        return GrpcTestSupport.completeVisit(visitGrpcController, request);
    }

    private VisitResponse cancelVisit(CancelVisitRequest request) {
        return GrpcTestSupport.cancelVisit(visitGrpcController, request);
    }

    private CheckOutVisitResponse checkOutVisit(CheckOutVisitRequest request) {
        return GrpcTestSupport.checkOutVisit(visitGrpcController, request);
    }

    private BillResponse getBill(long billId) {
        return GrpcTestSupport.getBill(billGrpcController, billId);
    }

    private MemberBillsResponse getMemberBills(long memberId) {
        return GrpcTestSupport.getMemberBills(billGrpcController, memberId);
    }

    private Map<String, Long> persistGyms(Map<String, GymRecord> gymMap) {
        Map<String, Long> gymIdsByName = new LinkedHashMap<>();

        for (Map.Entry<String, GymRecord> entry : gymMap.entrySet()) {
            String name = entry.getKey();
            GymRecord record = entry.getValue();

            gymIdsByName.put(name,
                    GrpcTestSupport.createGym(
                            gymGrpcController,
                            GrpcTestSupport.toCreateGymRequest(record)
                    ).getId());
        }

        return gymIdsByName;
    }

    private Map<String, Long> persistMembers(Map<String, MemberRecord> memberMap) {
        Map<String, Long> memberIdsByName = new LinkedHashMap<>();

        for (Map.Entry<String, MemberRecord> entry : memberMap.entrySet()) {
            String name = entry.getKey();
            MemberRecord record = entry.getValue();

            memberIdsByName.put(name,
                    GrpcTestSupport.createMember(
                            memberGrpcController,
                            GrpcTestSupport.toCreateMemberRequest(record)
                    ).getId());
        }

        return memberIdsByName;
    }

    private CreateVisitRequest buildCreateVisitRequest(long gymId, long memberId, Instant checkIn, double dailyRate) {
        return CreateVisitRequest.newBuilder()
                .setGymId(gymId)
                .setMemberId(memberId)
                .setCheckIn(Timestamp.newBuilder()
                        .setSeconds(checkIn.getEpochSecond())
                        .setNanos(checkIn.getNano())
                        .build())
                .setDailyRate(dailyRate)
                .setZone("101")
                .setStationCode("A")
                .build();
    }

    private CompleteVisitRequest buildCompleteVisitRequest(long visitId, Instant endDate) {
        return CompleteVisitRequest.newBuilder()
                .setId(visitId)
                .setEndDate(Timestamp.newBuilder()
                        .setSeconds(endDate.getEpochSecond())
                        .setNanos(endDate.getNano())
                        .build())
                .build();
    }

    private CheckOutVisitRequest buildCheckOutVisitRequest(long visitId, Instant checkOut) {
        return CheckOutVisitRequest.newBuilder()
                .setId(visitId)
                .setCheckOut(Timestamp.newBuilder()
                        .setSeconds(checkOut.getEpochSecond())
                        .setNanos(checkOut.getNano())
                        .build())
                .build();
    }
}
