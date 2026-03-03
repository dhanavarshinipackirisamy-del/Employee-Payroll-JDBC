package com.bridgelabz;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;

public class PayrollDBServiceTest {

    PayrollDBService service = PayrollDBService.getInstance();

    @Test
    public void givenEmployeePayrollDB_WhenRetrieved_ShouldReturnEmployeeList()
            throws PayrollException {

        List<EmployeePayroll> employees =
                service.readEmployeePayrollData();

        Assertions.assertEquals(3, employees.size());
    }

    @Test
    public void givenUpdatedSalary_WhenSynced_ShouldMatchWithDatabase()
            throws PayrollException {

        double oldSalary =
                service.getEmployeeData("Terisa").getBasicPay();

        service.updateEmployeeSalary("Terisa", 3200000.00);

        EmployeePayroll employee =
                service.getEmployeeData("Terisa");

        Assertions.assertEquals(3200000.00, employee.getBasicPay());

        // Restore original salary
        service.updateEmployeeSalary("Terisa", oldSalary);
    }
    @Test
    public void givenDateRange_WhenRetrieved_ShouldReturnCorrectEmployees()
            throws PayrollException {

        List<EmployeePayroll> employees =
                service.getEmployeesByDateRange(
                        LocalDate.of(2018, 1, 1),
                        LocalDate.now());

        Assertions.assertEquals(3, employees.size());
    }
    @Test
    public void givenEmployees_WhenGroupedByGender_ShouldReturnStatistics()
            throws PayrollException {

        List<PayrollStatistics> stats =
                service.getSalaryStatisticsByGender();

        Assertions.assertFalse(stats.isEmpty());
    }
}