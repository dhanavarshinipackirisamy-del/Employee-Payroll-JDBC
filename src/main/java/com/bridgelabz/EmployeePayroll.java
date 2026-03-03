package com.bridgelabz;

import java.time.LocalDate;

public class EmployeePayroll {

    private int employeeId;
    private String name;
    private double basicPay;
    private LocalDate startDate;

    public EmployeePayroll(int employeeId, String name, double basicPay, LocalDate startDate) {
        this.employeeId = employeeId;
        this.name = name;
        this.basicPay = basicPay;
        this.startDate = startDate;
    }

    @Override
    public String toString() {
        return "EmployeePayroll{" +
                "employeeId=" + employeeId +
                ", name='" + name + '\'' +
                ", basicPay=" + basicPay +
                ", startDate=" + startDate +
                '}';
    }
    public double getBasicPay() {
        return basicPay;
    }
}