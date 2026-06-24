package com.demo.gym.billing;

import com.demo.gym.controller.BillGrpcStatus;

public enum BillStatus {
    OPEN,
    CLOSED;

    public static BillGrpcStatus mapStatus(BillStatus status) {
        if (status == null) return BillGrpcStatus.STATUS_UNSPECIFIED;

        return switch (status) {
            case OPEN -> BillGrpcStatus.STATUS_OPEN;
            case CLOSED -> BillGrpcStatus.STATUS_CLOSED;
        };
    }
}
