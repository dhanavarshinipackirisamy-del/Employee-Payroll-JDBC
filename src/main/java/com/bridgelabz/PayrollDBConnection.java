package com.bridgelabz;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PayrollDBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/payroll_services";
    private static final String USER = "root";
    private static final String PASSWORD = "@SRMrmp26";

    public static void main(String[] args) {

        try {
            Connection connection =
                    DriverManager.getConnection(URL, USER, PASSWORD);

            if (connection != null) {
                System.out.println("Connection Established Successfully!");
            }

        } catch (SQLException e) {
            System.out.println("Connection Failed!");
            e.printStackTrace();
        }
    }
}