package com.boki.backend.domain.ai.entity;

public enum Grade {
    A, B, C, D, E;

    public static Grade from(double complianceRate) {
        if (complianceRate >= 90) return A;
        if (complianceRate >= 70) return B;
        if (complianceRate >= 50) return C;
        if (complianceRate >= 30) return D;
        return E;
    }
}
