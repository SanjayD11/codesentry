import sqlite3

def query_database(username):
    conn = sqlite3.connect('example.db')
    cursor = conn.cursor()
    
    # VULNERABLE: String formatting
    query = "SELECT * FROM users WHERE username = '%s'" % username
    cursor.execute(query)
    
    # VULNERABLE: f-string
    cursor.execute(f"SELECT * FROM users WHERE username = '{username}'")
