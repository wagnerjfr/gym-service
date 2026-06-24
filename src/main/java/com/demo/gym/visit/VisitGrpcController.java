package com.demo.gym.visit;

import com.demo.gym.common.Utils;
import com.demo.gym.controller.VisitServiceGrpc;
import com.demo.gym.controller.CreateVisitRequest;
import com.demo.gym.controller.VisitResponse;
import com.demo.gym.controller.CancelVisitRequest;
import com.demo.gym.controller.CompleteVisitRequest;
import com.demo.gym.controller.CheckOutVisitRequest;
import com.demo.gym.controller.CheckOutVisitResponse;
import com.demo.gym.controller.GetVisitRequest;
import com.demo.gym.controller.GetMemberVisitsRequest;
import com.demo.gym.controller.VisitGrpcStatus;
import com.demo.gym.controller.VisitListResponse;
import com.demo.gym.controller.GetGymVisitsRequest;
import com.demo.gym.controller.GetVisitSummaryQuarterRequest;
import com.demo.gym.controller.GetVisitSummaryQuarterResponse;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class VisitGrpcController extends VisitServiceGrpc.VisitServiceImplBase {

    private final VisitService visitService;

    @Override
    public void createVisit(CreateVisitRequest request, StreamObserver<VisitResponse> responseObserver) {
        try {
            Instant checkIn = null;
            if (request.hasCheckIn()) {
                checkIn = convertTimestamp(request.getCheckIn());
            }

            Visit visit = visitService.createVisit(
                    request.getGymId(),
                    request.getMemberId(),
                    checkIn,
                    request.getZone(),
                    request.getStationCode(),
                    BigDecimal.valueOf(request.getDailyRate())
            );

            VisitResponse response = buildVisitResponse(visit);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void cancelVisit(CancelVisitRequest request, StreamObserver<VisitResponse> responseObserver) {
        try {
            Visit visit = visitService.cancelVisit(request.getId(), request.getReason());
            VisitResponse response = buildVisitResponse(visit);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void completeVisit(CompleteVisitRequest request, StreamObserver<VisitResponse> responseObserver) {
        try {
            Instant endDate = null;
            if (request.hasEndDate()) {
                endDate = convertTimestamp(request.getEndDate());
            }

            Visit visit = visitService.completeVisit(request.getId(), endDate);
            VisitResponse response = buildVisitResponse(visit);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void checkOutVisit(CheckOutVisitRequest request, StreamObserver<CheckOutVisitResponse> responseObserver) {
        try {
            Instant checkOut = null;
            if (request.hasCheckOut()) {
                checkOut = convertTimestamp(request.getCheckOut());
            }

            Visit visit = visitService.checkOutVisit(request.getId(), checkOut);

            CheckOutVisitResponse response = CheckOutVisitResponse.newBuilder()
                    .setVisit(buildVisitResponse(visit))
                    .setBillId(visit.getBill().getId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getVisit(GetVisitRequest request, StreamObserver<VisitResponse> responseObserver) {
        try {
            Visit visit = visitService.getVisit(request.getId());
            VisitResponse response = buildVisitResponse(visit);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getMemberVisits(GetMemberVisitsRequest request, StreamObserver<VisitListResponse> responseObserver) {
        VisitStatus filter = null;
        if (request.getStatusFilter() != VisitGrpcStatus.UNSPECIFIED) {
            filter = VisitStatus.mapFromProto(request.getStatusFilter());
        }

        List<Visit> visits = visitService.getMemberVisits(request.getMemberId(), filter);

        VisitListResponse response = VisitListResponse.newBuilder()
                .addAllVisits(visits.stream().map(this::buildVisitResponse).toList())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getGymVisits(GetGymVisitsRequest request, StreamObserver<VisitListResponse> responseObserver) {
        VisitStatus filter = null;
        if (request.getStatusFilter() != VisitGrpcStatus.UNSPECIFIED) {
            filter = VisitStatus.mapFromProto(request.getStatusFilter());
        }

        List<Visit> visits = visitService.getGymVisits(request.getGymId(), filter);

        VisitListResponse response = VisitListResponse.newBuilder()
                .addAllVisits(visits.stream().map(this::buildVisitResponse).toList())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getVisitSummaryQuarter(GetVisitSummaryQuarterRequest request,
                                       StreamObserver<GetVisitSummaryQuarterResponse> responseObserver) {
        try {
            if (request.getYear() <= 0) {
                throw new IllegalArgumentException("year must be greater than 0");
            }
            if (request.getQuarter() < 1 || request.getQuarter() > 4) {
                throw new IllegalArgumentException("quarter must be between 1 and 4");
            }

            VisitService.VisitSummaryQuarter summary = visitService.getVisitSummaryQuarter(
                    request.getMemberId(),
                    request.getGymId(),
                    request.getYear(),
                    request.getQuarter()
            );

            GetVisitSummaryQuarterResponse response = GetVisitSummaryQuarterResponse.newBuilder()
                    .setMemberId(request.getMemberId())
                    .setGymId(request.getGymId())
                    .setYear(request.getYear())
                    .setQuarter(request.getQuarter())
                    .setTotalDays(summary.totalDays())
                    .setCurrency(summary.currency())
                    .setBilledAmount(summary.billedAmount().doubleValue())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    private VisitResponse buildVisitResponse(Visit visit) {
        VisitResponse.Builder builder = VisitResponse.newBuilder()
                .setId(visit.getId())
                .setGymId(visit.getGym().getId())
                .setMemberId(visit.getMember().getId())
                .setZone(visit.getZone() != null ? visit.getZone() : "")
                .setStationCode(visit.getStationCode() != null ? visit.getStationCode() : "")
                .setNote(visit.getNote() != null ? visit.getNote() : "")
                .setStatus(VisitStatus.mapToProto(visit.getStatus()))
                .setDailyRate(visit.getDailyRate() != null ? visit.getDailyRate().doubleValue() : 0.0)
                .setGymName(visit.getGym().getName())
                .setMemberName(visit.getMember().getName());

        if (visit.getBill() != null && visit.getBill().getId() != null) {
            builder.setBillId(visit.getBill().getId());
        }

        if (visit.getCheckIn() != null) {
            builder.setCheckIn(Utils.toTimestamp(visit.getCheckIn()));
        }

        if (visit.getCheckOut() != null) {
            builder.setCheckOut(Utils.toTimestamp(visit.getCheckOut()));
        }

        if (visit.getCreatedAt() != null) {
            builder.setCreatedAt(Utils.toTimestamp(visit.getCreatedAt()));
        }

        return builder.build();
    }

    private Instant convertTimestamp(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
