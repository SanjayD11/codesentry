public class VulnerableApp {
    public void runTests() {
        // Hardcoded Credentials
        String password = "super_secret_credentials_123";

        // Command Injection
        Runtime.getRuntime().exec("ping " + password);

        // Insecure Deserialization
        new ObjectInputStream(in);

        // Weak Cryptography
        MessageDigest.getInstance("MD5");

        // SQL Injection
        statement.executeQuery("SELECT * FROM users WHERE name = " + password);
    }
}
