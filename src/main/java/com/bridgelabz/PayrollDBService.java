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
}