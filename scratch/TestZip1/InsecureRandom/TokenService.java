import java.util.Random;
public class TokenService{
 Random r=new Random();
 public int token(){ return r.nextInt(); }
}