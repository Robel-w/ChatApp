package db;
import client.ChatGUI;

import java.sql.*;
public class ChatDB {
    private static final String url = "jdbc:mysql://localhost:3306/ChatApp?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String user = "root";
    private static final String password = "";

    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(url, user, password);
    }
    public static void saveMessage (String username, String message){
        String sql = "INSERT INTO messages(username, message) VALUES(?,?)";
        try (Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, username);
            stmt.setString(2, message);
            stmt.executeUpdate();

        }catch(Exception e){
                e.printStackTrace();
        }
    }

    public static void saveFile(String username, String filename, String base64Data) {
        String sql = "INSERT INTO messages(username, message) VALUES(?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, "[FILE] " + filename);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadRecentMessages(ChatGUI gui) {
        String sql = "SELECT username, message, sent_at FROM messages ORDER BY sent_at ASC LIMIT 50";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String msg = rs.getString("message");
                String user = rs.getString("username");
                gui.addMessageFromDB(user, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
