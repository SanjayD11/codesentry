import java.io.*;

public class SafeCommand {
    public void executeCommand(String userInput) throws Exception {
        // SAFE: Hardcoded string
        Runtime.getRuntime().exec("ping 127.0.0.1");
    }
}
