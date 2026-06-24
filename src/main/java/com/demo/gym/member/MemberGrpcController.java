package com.demo.gym.member;

import com.demo.gym.common.Address;
import com.demo.gym.controller.MemberServiceGrpc;
import com.demo.gym.controller.CreateMemberRequest;
import com.demo.gym.controller.MemberResponse;
import com.demo.gym.controller.GetMemberRequest;
import com.demo.gym.controller.ModifyMemberRequest;
import com.demo.gym.controller.DeleteMemberRequest;
import com.demo.gym.controller.DeleteResponse;
import com.demo.gym.controller.AddressMessage;
import com.google.type.Date;
import com.demo.gym.common.Utils;
import io.grpc.stub.StreamObserver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalDate;

@GrpcService
@RequiredArgsConstructor
public class MemberGrpcController extends MemberServiceGrpc.MemberServiceImplBase {

    private final MemberService memberService;

    @Override
    public void createMember(CreateMemberRequest request, StreamObserver<MemberResponse> responseObserver) {
        try {
            Member member = new Member();
            member.setName(request.getName());
            member.setTaxId(request.getTaxId());
            member.setEmail(request.getEmail());
            member.setPhoneNumber(request.getPhoneNumber());

            if (request.hasAddress()) {
                member.setAddress(mapAddress(request.getAddress()));
            }

            if (request.hasBirthDate()) {
                Date dob = request.getBirthDate();
                member.setBirthDate(LocalDate.of(dob.getYear(), dob.getMonth(), dob.getDay()));
            }

            member.setSex(BiologicalSex.mapSexFromProto(request.getSex()));

            Member saved = memberService.saveMember(member);
            responseObserver.onNext(Utils.buildMemberResponse(saved));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error creating member: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getMember(GetMemberRequest request, StreamObserver<MemberResponse> responseObserver) {
        try {
            Member member = memberService.getMember(request.getId());
            responseObserver.onNext(Utils.buildMemberResponse(member));
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void modifyMember(ModifyMemberRequest request, StreamObserver<MemberResponse> responseObserver) {
        try {
            Member updates = new Member();
            updates.setName(request.getName());
            updates.setEmail(request.getEmail());
            updates.setPhoneNumber(request.getPhoneNumber());

            if (request.hasAddress()) {
                updates.setAddress(mapAddress(request.getAddress()));
            }

            if (request.hasBirthDate()) {
                Date dob = request.getBirthDate();
                updates.setBirthDate(LocalDate.of(dob.getYear(), dob.getMonth(), dob.getDay()));
            }

            updates.setSex(BiologicalSex.mapSexFromProto(request.getSex()));

            Member saved = memberService.updateMember(request.getId(), updates);
            responseObserver.onNext(Utils.buildMemberResponse(saved));
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error modifying member: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteMember(DeleteMemberRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            boolean deleted = memberService.deleteMember(request.getId());

            DeleteResponse response = DeleteResponse.newBuilder()
                    .setSuccess(deleted)
                    .setMessage(deleted ? "Member deleted successfully" : "Member not found")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error deleting member: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private Address mapAddress(AddressMessage addr) {
        return new Address(
                addr.getCountry(),
                addr.getCity(),
                addr.getStreet(),
                addr.getZipCode()
        );
    }
}
