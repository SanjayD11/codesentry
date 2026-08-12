import java.io.*;
import java.sql.*;

public class SafeSQL {
    public void queryDatabase(String username) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "password");
        
        // SAFE: Prepared Statement
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
        pstmt.setString(1, username);
        pstmt.executeQuery();
    }
}
