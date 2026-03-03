package com.bridgelabz;

public class PayrollStatistics {

    private String gender;
    private double sum;
    private double avg;
    private double min;
    private double max;
    private int count;

    public PayrollStatistics(String gender, double sum,
                             double avg, double min,
                             double max, int count) {
        this.gender = gender;
        this.sum = sum;
        this.avg = avg;
        this.min = min;
        this.max = max;
        this.count = count;
    }

    @Override
    public String toString() {
        return "PayrollStatistics{" +
                "gender='" + gender + '\'' +
                ", sum=" + sum +
                ", avg=" + avg +
                ", min=" + min +
                ", max=" + max +
                ", count=" + count +
                '}';
    }
}