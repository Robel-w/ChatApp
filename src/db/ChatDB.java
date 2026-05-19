package db;

import client.ChatGUI;
import java.sql.*;

public class ChatDB {

    private static final String url = "jdbc:mysql://localhost:3306/ChatApp?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String user = "root";
    private static final String password = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
    public static void saveMessage(String username, String message) {
        saveMessageToDB(username, message, null, null);
    }
    public static void saveFile(String username, String filename, String base64Data) {
        saveMessageToDB(username, "[FILE] " + filename, filename, base64Data);
    }

    private static void saveMessageToDB(String username, String message,
                                        String fileName, String fileData) {
        String sql = "INSERT INTO messages (username, message, file_name, file_data) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, message);
            stmt.setString(3, fileName);
            stmt.setString(4, fileData);   // Base64 for files/images

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadRecentMessages(ChatGUI gui) {
        String sql = """
            SELECT username, message, file_name, file_data FROM messages ORDER BY sent_at ASC LIMIT 50
            """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String username = rs.getString("username");
                String message = rs.getString("message");
                String fileName = rs.getString("file_name");
                String fileData = rs.getString("file_data");

                if (fileName != null && fileData != null) {
                    gui.addFileFromDB(username, fileName, fileData);
                } else {
                    gui.addMessageFromDB(username, message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}