import java.nio.file.*;
public class FileController{
 public byte[] read(String name)throws Exception{
   return Files.readAllBytes(Paths.get("/uploads/"+name));
 }
}