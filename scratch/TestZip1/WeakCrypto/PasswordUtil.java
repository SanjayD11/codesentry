import java.security.*;
public class PasswordUtil{
 public String hash(String p)throws Exception{
  return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(p.getBytes()));
 }
}