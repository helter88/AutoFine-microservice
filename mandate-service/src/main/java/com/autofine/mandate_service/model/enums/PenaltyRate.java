package com.autofine.mandate_service.model.enums;

import java.math.BigDecimal;
import java.util.Arrays;

public enum PenaltyRate {

    UP_TO_10(BigDecimal.valueOf(100), 0, 10),
    FROM_11_TO_15(BigDecimal.valueOf(200), 11, 15),
    FROM_16_TO_20(BigDecimal.valueOf(300), 16, 20),
    FROM_21_TO_25(BigDecimal.valueOf(400), 21, 25),
    FROM_26_TO_30(BigDecimal.valueOf(500), 26, 30),
    OVER_30(BigDecimal.valueOf(800), 41, Integer.MAX_VALUE);

    private final BigDecimal amount;
    private final int minSpeed;
    private final int maxSpeed;

    PenaltyRate(BigDecimal amount, int minSpeed, int maxSpeed) {
        this.amount = amount;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getMinSpeed() {
        return minSpeed;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public static BigDecimal calculateFineAmount(int speedExcess) {
        return Arrays.stream(PenaltyRate.values())
                .filter(rate -> rate.getMinSpeed() <= speedExcess && rate.getMaxSpeed() >= speedExcess)
                .findFirst()
                .map(PenaltyRate::getAmount)
                .orElse(BigDecimal.ZERO);
    }
}
