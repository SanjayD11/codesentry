import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;

public class VulnerableXXE {
    public void parseXML(InputStream is) throws Exception {
        // VULNERABLE: Default parser allows external entities
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.parse(is);
    }
}
