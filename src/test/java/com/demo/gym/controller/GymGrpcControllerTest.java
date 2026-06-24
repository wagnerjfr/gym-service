package com.demo.gym.controller;
import com.demo.gym.gym.GymGrpcController;
import com.demo.gym.member.MemberGrpcController;

import static org.assertj.core.api.Assertions.assertThat;

import com.demo.gym.util.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GymGrpcControllerTest {

    @Autowired
    private GymGrpcController gymGrpcController;

    @Autowired
    private MemberGrpcController memberGrpcController;

    @Test
    void shouldCreateAndModifyAndDeleteGym() {
        CreateGymRequest request = TestEntityFactory.createGymRequest();
        final String sampleName = request.getName();
        final String sampleTaxId = request.getTaxId();
        final String sampleZipCode = request.getAddress().getZipCode();

        TestStreamObserver<GymResponse> responseObserver = new TestStreamObserver<>();
        gymGrpcController.createGym(request, responseObserver);

        assertThat(responseObserver.getError()).isNull();
        assertThat(responseObserver.getResponse()).isNotNull();
        final Long gymId = responseObserver.getResponse().getId();
        assertThat(gymId).isGreaterThan(0);
        assertThat(responseObserver.getResponse().getName()).isEqualTo(sampleName);
        assertThat(responseObserver.getResponse().getTaxId()).isEqualTo(sampleTaxId);
        assertThat(responseObserver.getResponse().getAddress().getZipCode()).isEqualTo(sampleZipCode);

        // Now let's modify
        final String email = "test@gym.com";
        final String phoneNumber = "123-456-7890";
        ModifyGymRequest modifyRequest = ModifyGymRequest.newBuilder()
                .setId(gymId)
                .setEmail(email)
                .setPhoneNumber(phoneNumber)
                .build();

        TestStreamObserver<GymResponse> modifyObserver = new TestStreamObserver<>();
        gymGrpcController.modifyGym(modifyRequest, modifyObserver);

        assertThat(modifyObserver.getResponse().getEmail()).isEqualTo(email);
        assertThat(modifyObserver.getResponse().getPhoneNumber()).isEqualTo(phoneNumber);

        // Now delete the gym
        DeleteGymRequest deleteRequest = DeleteGymRequest.newBuilder()
                .setId(gymId)
                .build();

        TestStreamObserver<DeleteResponse> deleteObserver = new TestStreamObserver<>();
        gymGrpcController.deleteGym(deleteRequest, deleteObserver);

        assertThat(deleteObserver.getError()).isNull();
        assertThat(deleteObserver.getResponse().getSuccess()).isTrue();
    }

    @Test
    void shouldRegisterMemberInGym() {
        // Create Member
        CreateMemberRequest createMemberRequest = TestEntityFactory.createMemberRequest();

        TestStreamObserver<MemberResponse> observerMember = new TestStreamObserver<>();
        memberGrpcController.createMember(createMemberRequest, observerMember);

        assertThat(observerMember.getResponse()).isNotNull();

        // Create Gym
        CreateGymRequest createGymRequest = TestEntityFactory.createGymRequest();

        TestStreamObserver<GymResponse> observerGym = new TestStreamObserver<>();
        gymGrpcController.createGym(createGymRequest, observerGym);

        assertThat(observerGym.getResponse()).isNotNull();

        // Create Registration
        final long gymId = observerGym.getResponse().getId();
        final long memberId = observerMember.getResponse().getId();

        RegisterMemberRequest registerMemberRequest = RegisterMemberRequest.newBuilder()
                .setGymId(gymId)
                .setMemberId(memberId)
                .build();

        TestStreamObserver<MembershipResponse> observerRegistration = new TestStreamObserver<>();
        gymGrpcController.registerMember(registerMemberRequest, observerRegistration);

        assertThat(observerRegistration.getError()).isNull();
        assertThat(observerRegistration.getResponse()).isNotNull();
        assertThat(observerRegistration.getResponse().getGymId()).isEqualTo(gymId);
        assertThat(observerRegistration.getResponse().getMemberId()).isEqualTo(memberId);

        // Verify member is in gym's member list
        GetGymRequest getGymRequest = GetGymRequest.newBuilder()
                .setId(gymId)
                .build();

        TestStreamObserver<MemberListResponse> memberListObserver = new TestStreamObserver<>();
        gymGrpcController.getGymMembers(getGymRequest, memberListObserver);

        assertThat(memberListObserver.getError()).isNull();
        assertThat(memberListObserver.getResponse().getMembersList())
                .hasSize(1)
                .anyMatch(p -> p.getId() == memberId);

        // Verify gym is in member's gym list
        GetMemberRequest getMemberRequest = GetMemberRequest.newBuilder()
                .setId(memberId)
                .build();

        TestStreamObserver<GymListResponse> gymListObserver = new TestStreamObserver<>();
        gymGrpcController.getMemberGyms(getMemberRequest, gymListObserver);

        assertThat(gymListObserver.getError()).isNull();
        assertThat(gymListObserver.getResponse().getGymsList())
                .hasSize(1)
                .anyMatch(h -> h.getId() == gymId);
    }
}
