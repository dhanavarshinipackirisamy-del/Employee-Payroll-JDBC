package com.bridgelabz;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class PayrollDBServiceTest {

    PayrollDBService service = PayrollDBService.getInstance();

    @BeforeEach
    void setupDatabase() throws SQLException {

        Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/payroll_services",
                "root",
                "@SRMrmp26");

        Statement stmt = con.createStatement();

        // 🔥 Clean tables
        stmt.executeUpdate("DELETE FROM payroll_details");
        stmt.executeUpdate("DELETE FROM payroll");
        stmt.executeUpdate("DELETE FROM employee_department");
        stmt.executeUpdate("DELETE FROM department");
        stmt.executeUpdate("DELETE FROM employee");

        // 🔥 Insert Departments
        stmt.executeUpdate("""
                INSERT INTO department (department_id, department_name)
                VALUES
                (1, 'HR'),
                (2, 'Sales'),
                (3, 'IT')
                """);

        // 🔥 Insert Employees
        stmt.executeUpdate("""
                INSERT INTO employee (employee_id, name, gender, start_date)
                VALUES
                (1, 'Terisa', 'F', '2019-01-01'),
                (2, 'Bill', 'M', '2018-05-10'),
                (3, 'Charlie', 'M', '2020-03-15')
                """);

        // 🔥 Insert Payroll
        stmt.executeUpdate("""
                INSERT INTO payroll (employee_id, basic_pay)
                VALUES
                (1, 3000000),
                (2, 2000000),
                (3, 2500000)
                """);

        // 🔥 Insert Payroll Details
        stmt.executeUpdate("""
                INSERT INTO payroll_details
                (employee_id, deductions, taxable_pay, income_tax, net_pay)
                VALUES
                (1, 600000, 2400000, 240000, 2160000),
                (2, 400000, 1600000, 160000, 1440000),
                (3, 500000, 2000000, 200000, 1800000)
                """);

        // 🔥 Map Employees to Departments
        stmt.executeUpdate("""
                INSERT INTO employee_department (employee_id, department_id)
                VALUES
                (1, 1),
                (2, 2),
                (3, 3)
                """);

        con.close();
    }

    // ✅ UC2 – Retrieve Employees
    @Test
    public void givenEmployeePayrollDB_WhenRetrieved_ShouldReturnEmployeeList()
            throws PayrollException {

        List<EmployeePayroll> employees =
                service.readEmployeePayrollData();

        Assertions.assertEquals(3, employees.size());
    }

    // ✅ UC4 – Update Salary
    @Test
    public void givenUpdatedSalary_WhenSynced_ShouldMatchWithDatabase()
            throws PayrollException {

        service.updateEmployeeSalary("Terisa", 3200000.00);

        EmployeePayroll employee =
                service.getEmployeeData("Terisa");

        Assertions.assertEquals(3200000.00,
                employee.getBasicPay());
    }



    // ✅ UC9 – Full ER Object Validation
    @Test
    public void givenEmployee_WhenRetrieved_ShouldContainDepartmentsAndPayrollDetails()
            throws PayrollException {

        EmployeePayroll employee =
                service.getEmployeeData("Terisa");

        Assertions.assertNotNull(employee);

        Assertions.assertNotNull(employee.getDepartments());
        Assertions.assertFalse(employee.getDepartments().isEmpty());

        Assertions.assertNotNull(employee.getPayrollDetails());
    }

    // ✅ UC9 – Transaction Test for Add Employee
    @Test
    public void givenNewEmployee_WhenAdded_ShouldBePresentWithFullDetails()
            throws PayrollException {

        EmployeePayroll employee =
                service.addEmployeeToPayroll(
                        "David",
                        2800000,
                        LocalDate.now(),
                        "M");

        EmployeePayroll dbEmployee =
                service.getEmployeeData("David");

        Assertions.assertNotNull(dbEmployee);
        Assertions.assertNotNull(dbEmployee.getPayrollDetails());
    }
}