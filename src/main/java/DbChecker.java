import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.ResultSetMetaData;

public class DbChecker {
    public static void main(String[] args) {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        
        System.out.println("Connecting to: " + url);
        
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE uploaded_files")) {
            
            System.out.println("Columns in uploaded_files:");
            while (rs.next()) {
                String field = rs.getString("Field");
                String type = rs.getString("Type");
                String isNull = rs.getString("Null");
                String key = rs.getString("Key");
                String def = rs.getString("Default");
                System.out.printf("- %-20s %-15s Null:%-4s Key:%-4s Default:%s%n", field, type, isNull, key, def);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
