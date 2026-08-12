import java.io.*;

public class VulnerableCommand {
    public void executeCommand(String userInput) throws Exception {
        // VULNERABLE: Direct execution of user input
        Runtime.getRuntime().exec("ping " + userInput);
    }
}
