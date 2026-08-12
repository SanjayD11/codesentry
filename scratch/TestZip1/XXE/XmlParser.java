import javax.xml.parsers.*;
public class XmlParser{
 public void parse(java.io.File f)throws Exception{
  DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
  dbf.newDocumentBuilder().parse(f);
 }
}