import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;

public class SafeXXE {
    public void parseXML(InputStream is) throws Exception {
        // SAFE: Explicitly disabling DTDs
        DocumentBuilderFactory safeFactory = DocumentBuilderFactory.newInstance();
        safeFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder safeBuilder = safeFactory.newDocumentBuilder();
        safeBuilder.parse(is);
    }
}
