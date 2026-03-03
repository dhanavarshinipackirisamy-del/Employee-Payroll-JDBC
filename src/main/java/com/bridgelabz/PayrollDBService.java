package com.bridgelabz;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PayrollDBService {

    private static PayrollDBService instance;

    private Connection connection;
    private PreparedStatement employeeByNameStatement;

    private static final String URL =
            "jdbc:mysql://localhost:3306/payroll_services";
    private static final String USER = "root";
    private static final String PASSWORD = "@SRMrmp26";

    // 🔒 Private Constructor
    private PayrollDBService() {
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);

            // PreparedStatement cached here
            String query = """
                    SELECT e.employee_id, e.name, p.basic_pay, e.start_date
                    FROM employee e
                    JOIN payroll p ON e.employee_id = p.employee_id
                    WHERE e.name = ?
                    """;

            this.employeeByNameStatement =
                    connection.prepareStatement(query);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 🔁 Singleton Instance
    public static PayrollDBService getInstance() {
        if (instance == null) {
            instance = new PayrollDBService();
        }
        return instance;
    }
    public EmployeePayroll getEmployeeData(String name) throws PayrollException {

        try {
            employeeByNameStatement.setString(1, name);

            ResultSet resultSet = employeeByNameStatement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToEmployee(resultSet);
            }

        } catch (SQLException e) {
            throw new PayrollException("Error retrieving employee: " + e.getMessage());
        }

        return null;
    }
    private EmployeePayroll mapResultSetToEmployee(ResultSet rs) throws SQLException {

        return new EmployeePayroll(
                rs.getInt("employee_id"),
                rs.getString("name"),
                rs.getDouble("basic_pay"),
                rs.getDate("start_date").toLocalDate()
        );
    }
    public List<EmployeePayroll> readEmployeePayrollData() throws PayrollException {

        String query = """
            SELECT e.employee_id, e.name, p.basic_pay, e.start_date
            FROM employee e
            JOIN payroll p ON e.employee_id = p.employee_id
            """;

        List<EmployeePayroll> employeeList = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                employeeList.add(mapResultSetToEmployee(resultSet));
            }

        } catch (SQLException e) {
            throw new PayrollException("Error retrieving payroll data");
        }

        return employeeList;
    }
    public void updateEmployeeSalary(String name, double newSalary)
            throws PayrollException {

        String updatePayrollQuery = """
            UPDATE payroll p
            JOIN employee e ON p.employee_id = e.employee_id
            SET p.basic_pay = ?
            WHERE e.name = ?
            """;

        String updateDetailsQuery =
                "UPDATE payroll_details SET deductions=?, taxable_pay=?, income_tax=?, net_pay=? WHERE employee_id=?";

        try {

            connection.setAutoCommit(false);

            // 1️⃣ Update basic pay
            PreparedStatement payrollStmt =
                    connection.prepareStatement(updatePayrollQuery);

            payrollStmt.setDouble(1, newSalary);
            payrollStmt.setString(2, name);
            payrollStmt.executeUpdate();

            // 2️⃣ Calculate derived values
            double deductions = newSalary * 0.20;
            double taxablePay = newSalary - deductions;
            double incomeTax = taxablePay * 0.10;
            double netPay = taxablePay - incomeTax;
            // 3️⃣ Get employee ID
            EmployeePayroll employee = getEmployeeData(name);
            int employeeId = employee.getEmployeeId();

            // 4️⃣ Update payroll_details
            PreparedStatement detailsStmt =
                    connection.prepareStatement(updateDetailsQuery);

            detailsStmt.setDouble(1, deductions);
            detailsStmt.setDouble(2, taxablePay);
            detailsStmt.setDouble(3, incomeTax);
            detailsStmt.setDouble(4, netPay);
            detailsStmt.setInt(5, employeeId);

            detailsStmt.executeUpdate();

            connection.commit();

        } catch (SQLException e) {

            try { connection.rollback(); } catch (SQLException ignored) {}

            throw new PayrollException("Error updating salary and payroll details");

        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }
    public EmployeePayroll addEmployeeToPayroll(String name,
                                                double salary,
                                                LocalDate startDate,
                                                String gender)
            throws PayrollException {

        String insertEmployeeQuery =
                "INSERT INTO employee (name, gender, start_date) VALUES (?, ?, ?)";

        String insertPayrollQuery =
                "INSERT INTO payroll (employee_id, basic_pay) VALUES (?, ?)";

        String insertPayrollDetailsQuery =
                "INSERT INTO payroll_details (employee_id, deductions, taxable_pay, income_tax, net_pay) VALUES (?, ?, ?, ?, ?)";

        try {

            connection.setAutoCommit(false);

            // 1️⃣ Insert employee
            PreparedStatement empStmt =
                    connection.prepareStatement(insertEmployeeQuery,
                            Statement.RETURN_GENERATED_KEYS);

            empStmt.setString(1, name);
            empStmt.setString(2, gender);
            empStmt.setDate(3, Date.valueOf(startDate));
            empStmt.executeUpdate();

            ResultSet keys = empStmt.getGeneratedKeys();
            int employeeId = 0;
            if (keys.next()) {
                employeeId = keys.getInt(1);
            }

            // 2️⃣ Insert payroll (basic pay)
            PreparedStatement payrollStmt =
                    connection.prepareStatement(insertPayrollQuery);

            payrollStmt.setInt(1, employeeId);
            payrollStmt.setDouble(2, salary);
            payrollStmt.executeUpdate();

            // 3️⃣ Calculate derived values
            double deductions = salary * 0.20;
            double taxablePay = salary - deductions;
            double incomeTax = taxablePay * 0.10;
            double netPay = salary - incomeTax;

            // 4️⃣ Insert payroll details
            PreparedStatement detailsStmt =
                    connection.prepareStatement(insertPayrollDetailsQuery);

            detailsStmt.setInt(1, employeeId);
            detailsStmt.setDouble(2, deductions);
            detailsStmt.setDouble(3, taxablePay);
            detailsStmt.setDouble(4, incomeTax);
            detailsStmt.setDouble(5, netPay);

            detailsStmt.executeUpdate();

            connection.commit();

            return new EmployeePayroll(employeeId, name, salary, startDate);

        } catch (SQLException e) {

            try { connection.rollback(); } catch (SQLException ignored) {}

            throw new PayrollException("Error adding employee with payroll details");

        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }
    public void deleteEmployee(String name) throws PayrollException {

        String deletePayrollQuery = """
            DELETE p FROM payroll p
            JOIN employee e ON p.employee_id = e.employee_id
            WHERE e.name = ?
            """;

        String deleteEmployeeQuery =
                "DELETE FROM employee WHERE name = ?";

        try {
            connection.setAutoCommit(false);

            PreparedStatement ps1 =
                    connection.prepareStatement(deletePayrollQuery);
            ps1.setString(1, name);
            ps1.executeUpdate();

            PreparedStatement ps2 =
                    connection.prepareStatement(deleteEmployeeQuery);
            ps2.setString(1, name);
            ps2.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            throw new PayrollException("Error deleting employee");
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }
    public PayrollDetails getPayrollDetails(String name)
            throws PayrollException {

        String query = """
            SELECT pd.deductions,
                   pd.taxable_pay,
                   pd.income_tax,
                   pd.net_pay
            FROM payroll_details pd
            JOIN employee e ON pd.employee_id = e.employee_id
            WHERE e.name = ?
            """;

        try (PreparedStatement ps =
                     connection.prepareStatement(query)) {

            ps.setString(1, name);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new PayrollDetails(
                        rs.getDouble("deductions"),
                        rs.getDouble("taxable_pay"),
                        rs.getDouble("income_tax"),
                        rs.getDouble("net_pay")
                );
            }

        } catch (SQLException e) {
            throw new PayrollException("Error retrieving payroll details");
        }

        return null;
    }
}