import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDB {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/defaultdb?serverTimezone=UTC", "root", "1234");
            Statement stmt = conn.createStatement();
            
            int updated = stmt.executeUpdate("UPDATE users SET password='$2a$10$.KfcwBFULu4RXBhiWxF6Ge23Q6bwhtgYG3JNT3KEt.qeVUEiO4D8S' WHERE email='testadmin@example.com'");
            System.out.println("Rows updated: " + updated);
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
