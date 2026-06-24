package com.demo.gym.billing;
import com.demo.gym.controller.BillServiceGrpc;
import com.demo.gym.controller.GetBillRequest;
import com.demo.gym.controller.BillResponse;
import com.demo.gym.controller.GetMemberBillsRequest;
import com.demo.gym.controller.MemberBillsResponse;
import com.demo.gym.controller.OutstandingBalanceRequest;
import com.demo.gym.controller.OutstandingBalanceResponse;
import com.demo.gym.common.Utils;

import io.grpc.stub.StreamObserver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class BillGrpcController extends BillServiceGrpc.BillServiceImplBase {

    private final BillService billService;

    @Override
    public void getBill(GetBillRequest request, StreamObserver<BillResponse> responseObserver) {
        try {
            Bill bill = billService.getBill(request.getId());
            responseObserver.onNext(toProto(bill));
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getMemberBills(GetMemberBillsRequest request, StreamObserver<MemberBillsResponse> responseObserver) {
        List<Bill> bills = billService.getMemberBills(request.getMemberId());

        MemberBillsResponse response = MemberBillsResponse.newBuilder()
                .addAllBills(bills.stream().map(this::toProto).toList())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getOutstandingBalance(OutstandingBalanceRequest request,
                                      StreamObserver<OutstandingBalanceResponse> responseObserver) {

        var amount = billService.getOutstandingBalance(request.getMemberId(), request.getGymId());

        OutstandingBalanceResponse response = OutstandingBalanceResponse.newBuilder()
                .setMemberId(request.getMemberId())
                .setGymId(request.getGymId())
                .setCurrency(BillService.EUR_CURRENCY)
                .setOutstandingAmount(amount.doubleValue())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private BillResponse toProto(Bill bill) {
        BillResponse.Builder builder = BillResponse.newBuilder()
                .setId(bill.getId())
                .setGymId(bill.getGym().getId())
                .setMemberId(bill.getMember().getId())
                .setStatus(BillStatus.mapStatus(bill.getStatus()))
                .setCurrency(BillService.EUR_CURRENCY)
                .setTotalAmount(bill.getTotalAmount() != null ? bill.getTotalAmount().doubleValue() : 0.0);

        if (bill.getIssueDate() != null) {
            builder.setIssueDate(Utils.toTimestamp(bill.getIssueDate()));
        }
        if (bill.getClosedAt() != null) {
            builder.setClosedAt(Utils.toTimestamp(bill.getClosedAt()));
        }

        return builder.build();
    }
}
