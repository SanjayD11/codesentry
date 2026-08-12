import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CheckPass {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$YlJW6.fZxwMwgm7PubIJuumZHIyrpJ6g7WcGINnCjWzjkXI8C6HLG";
        String[] passwords = {"admin", "admin123", "password", "testadmin", "123456"};
        for (String p : passwords) {
            if (encoder.matches(p, hash)) {
                System.out.println("Match found: " + p);
                return;
            }
        }
        System.out.println("No match found.");
    }
}
