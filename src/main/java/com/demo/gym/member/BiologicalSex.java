package com.demo.gym.member;

import com.demo.gym.controller.Sex;

public enum BiologicalSex {
    MALE,
    FEMALE,
    UNKNOWN,
    OTHER;

    public static BiologicalSex mapSexFromProto(Sex sex) {
        return switch (sex) {
            case SEX_MALE -> BiologicalSex.MALE;
            case SEX_FEMALE -> BiologicalSex.FEMALE;
            case SEX_OTHER -> BiologicalSex.OTHER;
            case SEX_UNSPECIFIED -> BiologicalSex.UNKNOWN;
            // MISSING: default case for SEX_UNRECOGNIZED
            default -> BiologicalSex.UNKNOWN;
        };
    }

    public static Sex mapSexToProto(BiologicalSex biologicalSex) {
        return switch (biologicalSex) {
            case MALE -> Sex.SEX_MALE;
            case FEMALE -> Sex.SEX_FEMALE;
            case OTHER -> Sex.SEX_OTHER;
            case UNKNOWN -> Sex.SEX_UNSPECIFIED;
        };
    }
}
