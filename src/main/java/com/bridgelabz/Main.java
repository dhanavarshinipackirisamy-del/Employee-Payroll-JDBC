package com.bridgelabz;

import java.time.LocalDate;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        PayrollDBService service = PayrollDBService.getInstance();

        try {

            // UC2 – Retrieve All Employees
            System.out.println("===== ALL EMPLOYEES =====");
            List<EmployeePayroll> allEmployees =
                    service.readEmployeePayrollData();
            allEmployees.forEach(System.out::println);



            // UC4 – Retrieve By Name

            System.out.println("\n===== RETRIEVE TERISA =====");
            EmployeePayroll terisa =
                    service.getEmployeeData("Terisa");
            System.out.println(terisa);



            // UC3 – Update Salary

            System.out.println("\n===== UPDATE TERISA SALARY =====");

            double oldSalary = terisa.getBasicPay();

            service.updateEmployeeSalary("Terisa", 3500000.00);

            EmployeePayroll updatedTerisa =
                    service.getEmployeeData("Terisa");

            System.out.println("After Update:");
            System.out.println(updatedTerisa);

            // Restore original salary
            service.updateEmployeeSalary("Terisa", oldSalary);





            // UC7 – Adding New Employee

            System.out.println("\n===== ADD NEW EMPLOYEE =====");

            EmployeePayroll newEmployee =
                    service.addEmployeeToPayroll(
                            "David",
                            2800000.00,
                            LocalDate.now(),
                            "M");

            System.out.println("Added:");
            System.out.println(newEmployee);

            // uc9

            System.out.println("\n===== ADD EMPLOYEE WITH PAYROLL DETAILS =====");

            EmployeePayroll emp =
                    service.addEmployeeToPayroll(
                            "Emma",
                            3000000,
                            LocalDate.now(),
                            "F");

            System.out.println(emp);

        } catch (PayrollException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}