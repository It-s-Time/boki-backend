package com.boki.backend.domain.ai.entity;

public enum Grade {
    S, A, B, C, F;

    public static Grade from(double complianceRate) {
        if (complianceRate >= 80) return S;
        if (complianceRate >= 60) return A;
        if (complianceRate >= 40) return B;
        if (complianceRate >= 20) return C;
        return F;
    }
}
