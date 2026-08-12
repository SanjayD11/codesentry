import sqlite3

def query_database(username):
    conn = sqlite3.connect('example.db')
    cursor = conn.cursor()
    
    # SAFE: Parameterized query
    cursor.execute("SELECT * FROM users WHERE username = ?", (username,))
