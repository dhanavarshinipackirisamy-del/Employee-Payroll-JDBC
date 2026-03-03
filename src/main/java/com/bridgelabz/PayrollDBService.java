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
    public void updateEmployeeSalary(String name, double newSalary) throws PayrollException {

        String query = """
            UPDATE payroll p
            JOIN employee e ON p.employee_id = e.employee_id
            SET p.basic_pay = ?
            WHERE e.name = ?
            """;

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(query)) {

            preparedStatement.setDouble(1, newSalary);
            preparedStatement.setString(2, name);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 0) {
                throw new PayrollException("Employee not found!");
            }

        } catch (SQLException e) {
            throw new PayrollException("Error updating salary: " + e.getMessage());
        }
    }
    public List<EmployeePayroll> getEmployeesByDateRange(LocalDate start, LocalDate end)
            throws PayrollException {

        String query = """
            SELECT e.employee_id, e.name, p.basic_pay, e.start_date
            FROM employee e
            JOIN payroll p ON e.employee_id = p.employee_id
            WHERE e.start_date BETWEEN ? AND ?
            """;

        List<EmployeePayroll> employeeList = new ArrayList<>();

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(query)) {

            preparedStatement.setDate(1, Date.valueOf(start));
            preparedStatement.setDate(2, Date.valueOf(end));

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                employeeList.add(mapResultSetToEmployee(resultSet));
            }

        } catch (SQLException e) {
            throw new PayrollException("Error retrieving employees by date range");
        }

        return employeeList;
    }
    public List<PayrollStatistics> getSalaryStatisticsByGender()
            throws PayrollException {

        String query = """
            SELECT e.gender,
                   SUM(p.basic_pay) AS total_salary,
                   AVG(p.basic_pay) AS average_salary,
                   MIN(p.basic_pay) AS min_salary,
                   MAX(p.basic_pay) AS max_salary,
                   COUNT(*) AS employee_count
            FROM employee e
            JOIN payroll p ON e.employee_id = p.employee_id
            GROUP BY e.gender
            """;

        List<PayrollStatistics> statisticsList = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                statisticsList.add(
                        new PayrollStatistics(
                                rs.getString("gender"),
                                rs.getDouble("total_salary"),
                                rs.getDouble("average_salary"),
                                rs.getDouble("min_salary"),
                                rs.getDouble("max_salary"),
                                rs.getInt("employee_count")
                        )
                );
            }

        } catch (SQLException e) {
            throw new PayrollException("Error retrieving salary statistics");
        }

        return statisticsList;
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

        try {

            connection.setAutoCommit(false); // Start transaction

            // 1️ Insert into employee
            PreparedStatement employeeStmt =
                    connection.prepareStatement(insertEmployeeQuery,
                            Statement.RETURN_GENERATED_KEYS);

            employeeStmt.setString(1, name);
            employeeStmt.setString(2, gender);
            employeeStmt.setDate(3, Date.valueOf(startDate));

            int rowsAffected = employeeStmt.executeUpdate();

            if (rowsAffected == 0)
                throw new PayrollException("Employee insert failed");

            ResultSet generatedKeys = employeeStmt.getGeneratedKeys();

            int employeeId = 0;
            if (generatedKeys.next()) {
                employeeId = generatedKeys.getInt(1);
            }

            // 2️ Insert into payroll
            PreparedStatement payrollStmt =
                    connection.prepareStatement(insertPayrollQuery);

            payrollStmt.setInt(1, employeeId);
            payrollStmt.setDouble(2, salary);

            payrollStmt.executeUpdate();

            connection.commit(); // commit transaction

            return new EmployeePayroll(employeeId,
                    name, salary, startDate);

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {}

            throw new PayrollException("Error adding employee");
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {}
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
}