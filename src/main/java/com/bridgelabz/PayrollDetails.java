package com.bridgelabz;

public class PayrollDetails {

    private double deductions;
    private double taxablePay;
    private double incomeTax;
    private double netPay;

    public PayrollDetails(double deductions,
                          double taxablePay,
                          double incomeTax,
                          double netPay) {
        this.deductions = deductions;
        this.taxablePay = taxablePay;
        this.incomeTax = incomeTax;
        this.netPay = netPay;
    }

    public double getDeductions() { return deductions; }
    public double getTaxablePay() { return taxablePay; }
    public double getIncomeTax() { return incomeTax; }
    public double getNetPay() { return netPay; }
}