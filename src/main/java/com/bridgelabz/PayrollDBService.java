package com.bridgelabz;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PayrollDBService {

    private static PayrollDBService instance;
    private Connection connection;

    private static final String URL =
            "jdbc:mysql://localhost:3306/payroll_services";
    private static final String USER = "root";
    private static final String PASSWORD = "@SRMrmp26";

    private PayrollDBService() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static PayrollDBService getInstance() {
        if (instance == null) {
            instance = new PayrollDBService();
        }
        return instance;
    }

    // =====================================================
    // READ ALL EMPLOYEES (UC9 READY)
    // =====================================================

    public List<EmployeePayroll> readEmployeePayrollData()
            throws PayrollException {

        List<EmployeePayroll> employeeList = new ArrayList<>();

        String query = """
                SELECT e.employee_id, e.name, e.start_date, p.basic_pay
                FROM employee e
                JOIN payroll p
                ON e.employee_id = p.employee_id
                """;

        try (PreparedStatement stmt =
                     connection.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                int id = rs.getInt("employee_id");
                String name = rs.getString("name");
                LocalDate startDate =
                        rs.getDate("start_date").toLocalDate();
                double salary = rs.getDouble("basic_pay");

                List<String> departments = getDepartments(id);
                PayrollDetails details =
                        getPayrollDetailsById(id);

                employeeList.add(
                        new EmployeePayroll(
                                id, name, salary,
                                startDate,
                                departments,
                                details));
            }

        } catch (SQLException e) {
            throw new PayrollException("Error reading employees");
        }

        return employeeList;
    }

    // =====================================================
    // GET SINGLE EMPLOYEE
    // =====================================================

    public EmployeePayroll getEmployeeData(String name)
            throws PayrollException {

        String query = """
                SELECT e.employee_id, e.name,
                       e.start_date, p.basic_pay
                FROM employee e
                JOIN payroll p
                ON e.employee_id = p.employee_id
                WHERE e.name = ?
                """;

        try (PreparedStatement stmt =
                     connection.prepareStatement(query)) {

            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                int id = rs.getInt("employee_id");

                return new EmployeePayroll(
                        id,
                        rs.getString("name"),
                        rs.getDouble("basic_pay"),
                        rs.getDate("start_date").toLocalDate(),
                        getDepartments(id),
                        getPayrollDetailsById(id));
            }

        } catch (SQLException e) {
            throw new PayrollException("Error retrieving employee");
        }

        return null;
    }

    // =====================================================
    // UPDATE SALARY (UC4 + UC8)
    // =====================================================

    public void updateEmployeeSalary(String name,
                                     double newSalary)
            throws PayrollException {

        try {

            connection.setAutoCommit(false);

            String updateSalary = """
                    UPDATE payroll p
                    JOIN employee e
                    ON p.employee_id = e.employee_id
                    SET p.basic_pay = ?
                    WHERE e.name = ?
                    """;

            PreparedStatement salaryStmt =
                    connection.prepareStatement(updateSalary);

            salaryStmt.setDouble(1, newSalary);
            salaryStmt.setString(2, name);
            salaryStmt.executeUpdate();

            EmployeePayroll emp =
                    getEmployeeData(name);

            int employeeId = emp.getEmployeeId();

            double deductions = newSalary * 0.20;
            double taxablePay = newSalary - deductions;
            double incomeTax = taxablePay * 0.10;
            double netPay = taxablePay - incomeTax; // FIXED

            String updateDetails = """
                    UPDATE payroll_details
                    SET deductions=?, taxable_pay=?,
                        income_tax=?, net_pay=?
                    WHERE employee_id=?
                    """;

            PreparedStatement detailStmt =
                    connection.prepareStatement(updateDetails);

            detailStmt.setDouble(1, deductions);
            detailStmt.setDouble(2, taxablePay);
            detailStmt.setDouble(3, incomeTax);
            detailStmt.setDouble(4, netPay);
            detailStmt.setInt(5, employeeId);

            detailStmt.executeUpdate();

            connection.commit();

        } catch (Exception e) {

            try { connection.rollback(); }
            catch (SQLException ignored) {}

            throw new PayrollException("Error updating salary");

        } finally {
            try { connection.setAutoCommit(true); }
            catch (SQLException ignored) {}
        }
    }

    // =====================================================
    // ADD EMPLOYEE (FULL TRANSACTION - UC9)
    // =====================================================

    public EmployeePayroll addEmployeeToPayroll(
            String name,
            double salary,
            LocalDate startDate,
            String gender)
            throws PayrollException {

        try {

            connection.setAutoCommit(false);

            String insertEmp =
                    "INSERT INTO employee (name, gender, start_date) VALUES (?, ?, ?)";

            PreparedStatement empStmt =
                    connection.prepareStatement(
                            insertEmp,
                            Statement.RETURN_GENERATED_KEYS);

            empStmt.setString(1, name);
            empStmt.setString(2, gender);
            empStmt.setDate(3, Date.valueOf(startDate));
            empStmt.executeUpdate();

            ResultSet keys =
                    empStmt.getGeneratedKeys();

            int employeeId = 0;
            if (keys.next())
                employeeId = keys.getInt(1);

            PreparedStatement payrollStmt =
                    connection.prepareStatement(
                            "INSERT INTO payroll (employee_id, basic_pay) VALUES (?, ?)");

            payrollStmt.setInt(1, employeeId);
            payrollStmt.setDouble(2, salary);
            payrollStmt.executeUpdate();

            double deductions = salary * 0.20;
            double taxablePay = salary - deductions;
            double incomeTax = taxablePay * 0.10;
            double netPay = taxablePay - incomeTax;

            PreparedStatement detailStmt =
                    connection.prepareStatement(
                            "INSERT INTO payroll_details VALUES (?, ?, ?, ?, ?)");

            detailStmt.setInt(1, employeeId);
            detailStmt.setDouble(2, deductions);
            detailStmt.setDouble(3, taxablePay);
            detailStmt.setDouble(4, incomeTax);
            detailStmt.setDouble(5, netPay);
            detailStmt.executeUpdate();

            connection.commit();

            return getEmployeeData(name);

        } catch (Exception e) {

            try { connection.rollback(); }
            catch (SQLException ignored) {}

            throw new PayrollException("Transaction failed");

        } finally {
            try { connection.setAutoCommit(true); }
            catch (SQLException ignored) {}
        }
    }

    // =====================================================
    // DELETE EMPLOYEE (CLEAN SAFE DELETE)
    // =====================================================

    public void deleteEmployee(String name)
            throws PayrollException {

        try {

            connection.setAutoCommit(false);

            int id = getEmployeeData(name)
                    .getEmployeeId();

            connection.prepareStatement(
                            "DELETE FROM payroll_details WHERE employee_id=" + id)
                    .executeUpdate();

            connection.prepareStatement(
                            "DELETE FROM payroll WHERE employee_id=" + id)
                    .executeUpdate();

            connection.prepareStatement(
                            "DELETE FROM employee_department WHERE employee_id=" + id)
                    .executeUpdate();

            connection.prepareStatement(
                            "DELETE FROM employee WHERE employee_id=" + id)
                    .executeUpdate();

            connection.commit();

        } catch (Exception e) {

            try { connection.rollback(); }
            catch (SQLException ignored) {}

            throw new PayrollException("Delete failed");

        } finally {
            try { connection.setAutoCommit(true); }
            catch (SQLException ignored) {}
        }
    }

    // =====================================================
    // DEPARTMENTS
    // =====================================================

    private List<String> getDepartments(int employeeId)
            throws SQLException {

        List<String> departments = new ArrayList<>();

        String query = """
                SELECT d.department_name
                FROM department d
                JOIN employee_department ed
                ON d.department_id = ed.department_id
                WHERE ed.employee_id = ?
                """;

        PreparedStatement stmt =
                connection.prepareStatement(query);

        stmt.setInt(1, employeeId);

        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            departments.add(rs.getString("department_name"));
        }

        return departments;
    }

    // =====================================================
    // PAYROLL DETAILS
    // =====================================================

    private PayrollDetails getPayrollDetailsById(int employeeId)
            throws SQLException {

        String query = """
                SELECT deductions, taxable_pay,
                       income_tax, net_pay
                FROM payroll_details
                WHERE employee_id = ?
                """;

        PreparedStatement stmt =
                connection.prepareStatement(query);

        stmt.setInt(1, employeeId);

        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return new PayrollDetails(
                    rs.getDouble("deductions"),
                    rs.getDouble("taxable_pay"),
                    rs.getDouble("income_tax"),
                    rs.getDouble("net_pay"));
        }

        return null;
    }
}