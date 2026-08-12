import java.sql.*;
public class LoginController {
  public void login(Connection c,String u,String p) throws Exception{
    Statement st=c.createStatement();
    st.executeQuery("SELECT * FROM users WHERE username='"+u+"' AND password='"+p+"'");
  }
}