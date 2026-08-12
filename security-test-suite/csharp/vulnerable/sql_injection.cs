using System.Data.SqlClient;

public class SecurityTest
{
    public void QueryDb(string username)
    {
        using (SqlConnection conn = new SqlConnection("Server=myServer;Database=myDB;"))
        {
            conn.Open();
            // VULNERABLE: SQL Injection
            string query = "SELECT * FROM Users WHERE Username = '" + username + "'";
            SqlCommand cmd = new SqlCommand(query, conn);
            cmd.ExecuteReader();
        }
    }
}
