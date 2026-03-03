package com.bridgelabz;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PayrollDBServiceTest {

    PayrollDBService service = new PayrollDBService();

    @Test
    public void givenEmployeePayrollDB_WhenRetrieved_ShouldReturnEmployeeList() throws PayrollException {

        List<EmployeePayroll> employees =
                service.readEmployeePayrollData();

        Assertions.assertEquals(3, employees.size());
    }
    @Test
    public void givenNewSalaryForTerisa_WhenUpdated_ShouldSyncWithDatabase() throws PayrollException {

        PayrollDBService service = new PayrollDBService();

        boolean updated = service.updateEmployeeSalary("Terisa", 3000000.00);
        Assertions.assertTrue(updated);

        double salary = service.getEmployeeSalary("Terisa");

        Assertions.assertEquals(3000000.00, salary);
    }
}