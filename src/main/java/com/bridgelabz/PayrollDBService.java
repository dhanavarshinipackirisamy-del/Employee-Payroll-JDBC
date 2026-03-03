package com.bridgelabz;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PayrollDBService {

    private static final String URL =
            "jdbc:mysql://localhost:3306/payroll_services";
    private static final String USER = "root";
    private static final String PASSWORD = "@SRMrmp26";

    public List<EmployeePayroll> readEmployeePayrollData() throws PayrollException {

        List<EmployeePayroll> employeeList = new ArrayList<>();

        String query = """
                SELECT e.employee_id, e.name, p.basic_pay, e.start_date
                FROM employee e
                JOIN payroll p
                ON e.employee_id = p.employee_id
                """;

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {

                int id = resultSet.getInt("employee_id");
                String name = resultSet.getString("name");
                double salary = resultSet.getDouble("basic_pay");
                LocalDate startDate = resultSet.getDate("start_date").toLocalDate();

                EmployeePayroll employee =
                        new EmployeePayroll(id, name, salary, startDate);

                employeeList.add(employee);
            }

        } catch (SQLException e) {
            throw new PayrollException("Error retrieving payroll data: " + e.getMessage());
        }

        return employeeList;
    }
    public boolean updateEmployeeSalary(String name, double newSalary) throws PayrollException {

        String query = """
            UPDATE payroll p
            JOIN employee e ON p.employee_id = e.employee_id
            SET p.basic_pay = ?
            WHERE e.name = ?
            """;

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setDouble(1, newSalary);
            preparedStatement.setString(2, name);

            int rowsAffected = preparedStatement.executeUpdate();

            return rowsAffected > 0;

        } catch (SQLException e) {
            throw new PayrollException("Unable to update salary: " + e.getMessage());
        }
    }
    public double getEmployeeSalary(String name) throws PayrollException {

        String query = """
            SELECT p.basic_pay
            FROM payroll p
            JOIN employee e ON p.employee_id = e.employee_id
            WHERE e.name = ?
            """;

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, name);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getDouble("basic_pay");
            }

        } catch (SQLException e) {
            throw new PayrollException("Error fetching salary: " + e.getMessage());
        }

        return 0;
    }
}