package com.demo.gym.controller;
import com.demo.gym.member.MemberGrpcController;

import static org.assertj.core.api.Assertions.assertThat;

import com.demo.gym.util.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext
class MemberGrpcControllerTest {

    @Autowired
    private MemberGrpcController memberGrpcController;

    @Test
    void shouldCreateAndDeleteMember() {
        CreateMemberRequest request = TestEntityFactory.createMemberRequest();
        final String sampleName = request.getName();
        final String sampleTaxId = request.getTaxId();
        final String sampleZipCode = request.getAddress().getZipCode();

        TestStreamObserver<MemberResponse> responseObserver = new TestStreamObserver<>();
        memberGrpcController.createMember(request, responseObserver);

        assertThat(responseObserver.getError()).isNull();
        assertThat(responseObserver.getResponse()).isNotNull();
        final Long memberId = responseObserver.getResponse().getId();
        assertThat(memberId).isGreaterThan(0);
        assertThat(responseObserver.getResponse().getName()).isEqualTo(sampleName);
        assertThat(responseObserver.getResponse().getTaxId()).isEqualTo(sampleTaxId);
        assertThat(responseObserver.getResponse().getAddress().getZipCode()).isEqualTo(sampleZipCode);
        assertThat(responseObserver.getResponse().getBirthDate().getYear()).isEqualTo(1990);
        assertThat(responseObserver.getResponse().getBirthDate().getMonth()).isEqualTo(5);
        assertThat(responseObserver.getResponse().getBirthDate().getDay()).isEqualTo(15);
        assertThat(responseObserver.getResponse().getSex()).isEqualTo(Sex.SEX_MALE);

        // Now let's modify
        final String email = "test@gym.com";
        final String phoneNumber = "123-456-7890";
        ModifyMemberRequest modifyRequest = ModifyMemberRequest.newBuilder()
                .setId(memberId)
                .setEmail(email)
                .setPhoneNumber(phoneNumber)
                .build();

        TestStreamObserver<MemberResponse> modifyObserver = new TestStreamObserver<>();
        memberGrpcController.modifyMember(modifyRequest, modifyObserver);

        assertThat(modifyObserver.getResponse().getEmail()).isEqualTo(email);
        assertThat(modifyObserver.getResponse().getPhoneNumber()).isEqualTo(phoneNumber);

        // Now delete the member
        DeleteMemberRequest deleteRequest = DeleteMemberRequest.newBuilder()
                .setId(responseObserver.getResponse().getId())
                .build();

        TestStreamObserver<DeleteResponse> deleteObserver = new TestStreamObserver<>();
        memberGrpcController.deleteMember(deleteRequest, deleteObserver);

        assertThat(deleteObserver.getError()).isNull();
        assertThat(deleteObserver.getResponse().getSuccess()).isTrue();
    }
}