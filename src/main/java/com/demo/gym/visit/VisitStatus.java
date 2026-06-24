package com.demo.gym.visit;

import com.demo.gym.controller.VisitGrpcStatus;

public enum VisitStatus {
    CHECKED_IN,
    COMPLETED,
    CHECKED_OUT,
    CANCELLED;

    public static VisitStatus mapFromProto(VisitGrpcStatus protoStatus) {
        return switch (protoStatus) {
            case CHECKED_IN -> CHECKED_IN;
            case COMPLETED -> COMPLETED;
            case CHECKED_OUT -> CHECKED_OUT;
            case CANCELLED -> CANCELLED;
            default -> throw new IllegalArgumentException("Unknown status: " + protoStatus);
        };
    }

    public static VisitGrpcStatus mapToProto(VisitStatus status) {
        return switch (status) {
            case CHECKED_IN -> VisitGrpcStatus.CHECKED_IN;
            case COMPLETED -> VisitGrpcStatus.COMPLETED;
            case CHECKED_OUT -> VisitGrpcStatus.CHECKED_OUT;
            case CANCELLED -> VisitGrpcStatus.CANCELLED;
        };
    }
}