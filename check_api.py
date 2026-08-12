import urllib.request
import json

url = 'http://localhost:8081/api/v1/auth/register'
data = json.dumps({
    "email": "sanjaydharmarajou@gmail.com",
    "password": "Test@123",
    "firstName": "Sanjay",
    "lastName": "Dharmarajou"
}).encode('utf-8')

req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})

try:
    with urllib.request.urlopen(req) as f:
        print("Success:", f.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("HTTP Error:", e.code, e.read().decode('utf-8'))
except Exception as e:
    print("Error:", e)
