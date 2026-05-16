package db;

import java.sql.*;

public class Testconn {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/ChatApp?useSSL=false&allowPublicKeyRetrieval=true";

        try (Connection conn = DriverManager.getConnection(url, "root", "")) {
            System.out.println("✅ Database Connected Successfully!");
        } catch (Exception e) {
            System.out.println("❌ Connection Failed!");
            e.printStackTrace();
        }
    }
}