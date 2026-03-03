package com.bridgelabz;

import java.time.LocalDate;
import java.util.List;

public class EmployeePayroll {

    private int employeeId;
    private String name;
    private double basicPay;
    private LocalDate startDate;
    private List<String> departments;
    private PayrollDetails payrollDetails;

    public EmployeePayroll(int employeeId,
                           String name,
                           double basicPay,
                           LocalDate startDate,
                           List<String> departments,
                           PayrollDetails payrollDetails) {

        this.employeeId = employeeId;
        this.name = name;
        this.basicPay = basicPay;
        this.startDate = startDate;
        this.departments = departments;
        this.payrollDetails = payrollDetails;
    }

    public int getEmployeeId() { return employeeId; }
    public String getName() { return name; }
    public double getBasicPay() { return basicPay; }
    public LocalDate getStartDate() { return startDate; }
    public List<String> getDepartments() { return departments; }
    public PayrollDetails getPayrollDetails() { return payrollDetails; }

    @Override
    public String toString() {
        return "EmployeePayroll{" +
                "id=" + employeeId +
                ", name='" + name + '\'' +
                ", salary=" + basicPay +
                ", departments=" + departments +
                '}';
    }
}