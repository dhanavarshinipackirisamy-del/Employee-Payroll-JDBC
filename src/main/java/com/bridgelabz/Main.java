package com.bridgelabz;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        PayrollDBService service = new PayrollDBService();

        try {
            List<EmployeePayroll> employees =
                    service.readEmployeePayrollData();

            employees.forEach(System.out::println);

        } catch (PayrollException e) {
            System.out.println(e.getMessage());
        }
    }
}