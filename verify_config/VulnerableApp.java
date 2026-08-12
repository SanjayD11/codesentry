import java.sql.Connection;
import java.sql.Statement;
import javax.crypto.Cipher;
import java.security.MessageDigest;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;

// TEST FILE: contains one deliberate instance of every vulnerability category

public class VulnerableApp {

    // --- HARDCODED SECRETS (category: Identification and Authentication Failures) ---
    private static final String DB_PASSWORD = "mysecretpassword123";
    private static final String API_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String JWT_SECRET = "weakjwtsecret";

    // --- WEAK CRYPTOGRAPHY (category: Cryptographic Failures) ---
    public static String hashPassword(String pw) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");   // MD5 is weak
        byte[] digest = md.digest(pw.getBytes());
        return new String(digest);
    }

    // --- SQL INJECTION (category: Injection) ---
    public static void getUser(Connection conn, String userId) throws Exception {
        Statement stmt = conn.createStatement();
        String query = "SELECT * FROM users WHERE id = " + userId;  // SQL injection
        stmt.executeQuery(query);
    }

    // --- XSS (category: Injection) ---
    public static String render(String userInput) {
        return "<div>" + userInput + "</div>";   // XSS: unsanitised output
    }

    // --- COMMAND INJECTION (category: Injection) ---
    public static void runCmd(String filename) throws Exception {
        Runtime.getRuntime().exec("ls -la " + filename);   // command injection
    }

    // --- PATH TRAVERSAL (category: Broken Access Control) ---
    public static byte[] readFile(String name) throws Exception {
        File f = new File("/uploads/" + name);    // path traversal
        InputStream in = new FileInputStream(f);
        return in.readAllBytes();
    }

    // --- INSECURE DESERIALIZATION (category: Software and Data Integrity Failures) ---
    public static Object deserialize(byte[] data) throws Exception {
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
            new java.io.ByteArrayInputStream(data));   // insecure deserialization
        return ois.readObject();
    }
}
