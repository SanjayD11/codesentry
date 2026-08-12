import mysql.connector

try:
    conn = mysql.connector.connect(
        host="localhost",
        user="root",
        password="1234",
        database="defaultdb"
    )
    cursor = conn.cursor()
    cursor.execute("SELECT id, email, role, active, email_verified FROM users")
    rows = cursor.fetchall()
    print("Users found:")
    for row in rows:
        print(row)
    conn.close()
except Exception as e:
    print("Error:", e)
