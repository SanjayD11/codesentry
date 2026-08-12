import java.io.*;
import java.sql.*;

public class VulnerableSQL {
    public void queryDatabase(String username) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "password");
        Statement stmt = conn.createStatement();
        
        // VULNERABLE: String concatenation
        String query = "SELECT * FROM users WHERE username = '" + username + "'";
        stmt.executeQuery(query);
    }
}
