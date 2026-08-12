$url = "http://localhost:8081/api/v1/scan"
$token = ""

# Since we don't have a token, we can just login!
$loginUrl = "http://localhost:8081/api/v1/auth/login"
$loginBody = @{
    email = "admin@example.com"
    password = "password123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginResponse.data.token

$headers = @{
    "Authorization" = "Bearer $token"
}

$scanBody = @{
    projectId = 3
} | ConvertTo-Json

Invoke-RestMethod -Uri $url -Method Post -Body $scanBody -Headers $headers -ContentType "application/json"
