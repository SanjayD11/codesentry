using System.Data.SqlClient;

public class SecurityTest
{
    public void QueryDbSafe(string username)
    {
        using (SqlConnection conn = new SqlConnection("Server=myServer;Database=myDB;"))
        {
            conn.Open();
            // SAFE: Parameterized Query
            SqlCommand cmd = new SqlCommand("SELECT * FROM Users WHERE Username = @username", conn);
            cmd.Parameters.AddWithValue("@username", username);
            cmd.ExecuteReader();
        }
    }
}
