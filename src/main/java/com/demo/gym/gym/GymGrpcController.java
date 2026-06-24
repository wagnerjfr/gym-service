package com.demo.gym.gym;

import com.demo.gym.common.Address;
import com.demo.gym.controller.GymServiceGrpc;
import com.demo.gym.controller.CreateGymRequest;
import com.demo.gym.controller.GymResponse;
import com.demo.gym.controller.AddressMessage;
import com.demo.gym.controller.GetGymRequest;
import com.demo.gym.controller.ModifyGymRequest;
import com.demo.gym.controller.DeleteGymRequest;
import com.demo.gym.controller.DeleteResponse;
import com.demo.gym.controller.RegisterMemberRequest;
import com.demo.gym.controller.MembershipResponse;
import com.demo.gym.controller.MemberListResponse;
import com.demo.gym.controller.GetMemberRequest;
import com.demo.gym.controller.GymListResponse;
import com.demo.gym.member.GymMember;
import com.demo.gym.member.Member;
import com.demo.gym.member.GymMemberService;
import com.demo.gym.common.Utils;
import io.grpc.stub.StreamObserver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService // Using the official Spring gRPC annotation
@RequiredArgsConstructor
public class GymGrpcController extends GymServiceGrpc.GymServiceImplBase {

    private final GymService gymService;
    private final GymMemberService gymMemberService;

    @Override
    public void createGym(CreateGymRequest request, StreamObserver<GymResponse> responseObserver) {
        try {
            Gym gym = new Gym();
            gym.setName(request.getName());
            gym.setTaxId(request.getTaxId());
            gym.setEmail(request.getEmail());
            gym.setPhoneNumber(request.getPhoneNumber());
            gym.setWebsiteUrl(request.getWebsiteUrl());
            gym.setCapacity(request.getCapacity());

            if (request.hasAddress()) {
                AddressMessage addr = request.getAddress();
                gym.setAddress(new Address(
                        addr.getCountry(),
                        addr.getCity(),
                        addr.getStreet(),
                        addr.getZipCode()
                ));
            }

            Gym saved = gymService.saveGym(gym);

            GymResponse response = buildGymResponse(saved);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error creating gym: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getGym(GetGymRequest request, StreamObserver<GymResponse> responseObserver) {
        try {
            Gym gym = gymService.getGym(request.getId());

            AddressMessage.Builder addressBuilder = AddressMessage.newBuilder();
            if (gym.getAddress() != null) {
                addressBuilder
                        .setCountry(gym.getAddress().country())
                        .setCity(gym.getAddress().city())
                        .setStreet(gym.getAddress().street())
                        .setZipCode(gym.getAddress().zipCode());
            }

            GymResponse response = buildGymResponse(gym);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void modifyGym(ModifyGymRequest request, StreamObserver<GymResponse> responseObserver) {
        try {
            Gym updates = new Gym();
            updates.setName(request.getName());
            updates.setEmail(request.getEmail());
            updates.setPhoneNumber(request.getPhoneNumber());
            updates.setWebsiteUrl(request.getWebsiteUrl());
            updates.setCapacity(request.getCapacity());

            if (request.hasAddress()) {
                AddressMessage addr = request.getAddress();
                updates.setAddress(new Address(
                        addr.getCountry(),
                        addr.getCity(),
                        addr.getStreet(),
                        addr.getZipCode()
                ));
            }

            Gym saved = gymService.updateGym(request.getId(), updates);

            GymResponse response = buildGymResponse(saved);
            responseObserver.onNext(response);
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
                    .withDescription("Error modifying gym: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteGym(DeleteGymRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            boolean deleted = gymService.deleteGym(request.getId());

            DeleteResponse response = DeleteResponse.newBuilder()
                    .setSuccess(deleted)
                    .setMessage(deleted ? "Gym deleted successfully" : "Gym not found")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error deleting gym: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // Helper method to build response
    private GymResponse buildGymResponse(Gym saved) {
        AddressMessage.Builder addressBuilder = AddressMessage.newBuilder();
        if (saved.getAddress() != null) {
            addressBuilder
                    .setCountry(saved.getAddress().country())
                    .setCity(saved.getAddress().city())
                    .setStreet(saved.getAddress().street())
                    .setZipCode(saved.getAddress().zipCode());
        }

        return GymResponse.newBuilder()
                .setId(saved.getId())
                .setName(saved.getName())
                .setTaxId(saved.getTaxId())
                .setEmail(saved.getEmail())
                .setPhoneNumber(saved.getPhoneNumber())
                .setWebsiteUrl(saved.getWebsiteUrl())
                .setCapacity(saved.getCapacity())
                .setAddress(addressBuilder.build())
                .build();
    }

    @Override
    public void registerMember(RegisterMemberRequest request, StreamObserver<MembershipResponse> responseObserver) {
        try {
            GymMember registration = gymMemberService.registerMember(
                    request.getGymId(),
                    request.getMemberId()
            );

            MembershipResponse response = MembershipResponse.newBuilder()
                    .setGymId(registration.getGym().getId())
                    .setMemberId(registration.getMember().getId())
                    .setStatus(registration.getStatus().name())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalStateException e) {
            responseObserver.onError(io.grpc.Status.ALREADY_EXISTS
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getGymMembers(GetGymRequest request, StreamObserver<MemberListResponse> responseObserver) {
        try {
            List<Member> members = gymMemberService.getGymMembers(request.getId());

            MemberListResponse response = MemberListResponse.newBuilder()
                    .addAllMembers(members.stream()
                            .map(Utils::buildMemberResponse)
                            .toList())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error retrieving gym members: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getMemberGyms(GetMemberRequest request, StreamObserver<GymListResponse> responseObserver) {
        try {
            List<Gym> gyms = gymMemberService.getMemberGyms(request.getId());

            GymListResponse response = GymListResponse.newBuilder()
                    .addAllGyms(gyms.stream()
                            .map(this::buildGymResponse)
                            .toList())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EntityNotFoundException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error retrieving member gyms: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
