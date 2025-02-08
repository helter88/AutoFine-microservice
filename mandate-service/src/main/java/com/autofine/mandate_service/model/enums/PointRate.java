package com.autofine.mandate_service.model.enums;

public enum PointRate {
    UP_TO_10(1, 0, 10),
    FROM_11_TO_15(2, 11, 15),
    FROM_16_TO_20(3, 16, 20),
    FROM_21_TO_25(5, 21, 25),
    FROM_26_TO_30(7, 26, 30),
    OVER_30(10, 31, Integer.MAX_VALUE);

    private final int points;
    private final int minSpeed;
    private final int maxSpeed;

    PointRate(int points, int minSpeed, int maxSpeed) {
        this.points = points;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
    }

    public int getPoints() {
        return points;
    }

    public int getMinSpeed() {
        return minSpeed;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }
}
