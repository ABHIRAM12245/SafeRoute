param (
    [string]$ProjectId = "routeguard-93561"
)

$ErrorActionPreference = "Stop"

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host " Deploying firestore.rules to project: $ProjectId" -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan

# Find gcloud executable
$gcloudPath = "C:\Users\ASUS\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
if (-not (Test-Path $gcloudPath)) {
    $gcloudCmd = Get-Command gcloud -ErrorAction SilentlyContinue
    if ($gcloudCmd) {
        $gcloudPath = $gcloudCmd.Source
    } else {
        Write-Error "gcloud CLI not found. Please install Google Cloud SDK or use the Firebase Console."
        exit 1
    }
}

# 1. Get access token
Write-Host "[1/3] Checking gcloud authentication..." -ForegroundColor Yellow
$token = & $gcloudPath auth print-access-token 2>$null

if (-not $token) {
    Write-Host "No active gcloud session found. Launching browser login..." -ForegroundColor Yellow
    & $gcloudPath auth login --brief
    $token = & $gcloudPath auth print-access-token
}

if (-not $token) {
    Write-Error "Could not retrieve access token. Run 'gcloud auth login' and try again."
    exit 1
}

$token = $token.Trim()
Write-Host "  -> Authenticated successfully." -ForegroundColor Green

# 2. Read firestore.rules
$rulesFilePath = Join-Path $PSScriptRoot "firestore.rules"
if (-not (Test-Path $rulesFilePath)) {
    Write-Error "firestore.rules file not found at: $rulesFilePath"
    exit 1
}
$rulesContent = Get-Content -Raw -Path $rulesFilePath

# 3. Create ruleset via Firebase Rules REST API
Write-Host "`n[2/3] Uploading and compiling firestore.rules..." -ForegroundColor Yellow
$rulesetPayload = @{
    source = @{
        files = @(
            @{
                name = "firestore.rules"
                content = $rulesContent
            }
        )
    }
} | ConvertTo-Json -Depth 5

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/json"
}

$rulesetUrl = "https://firebaserules.googleapis.com/v1/projects/$ProjectId/rulesets"
try {
    $rulesetResponse = Invoke-RestMethod -Uri $rulesetUrl -Method Post -Headers $headers -Body $rulesetPayload
    $rulesetName = $rulesetResponse.name
    Write-Host "  -> Ruleset compiled and created: $rulesetName" -ForegroundColor Green
} catch {
    Write-Error "Failed to create ruleset: $_"
    exit 1
}

# 4. Release to Cloud Firestore
Write-Host "`n[3/3] Releasing ruleset to Cloud Firestore database..." -ForegroundColor Yellow
$releasePayload = @{
    release = @{
        name = "projects/$ProjectId/releases/cloud.firestore"
        rulesetName = $rulesetName
    }
} | ConvertTo-Json -Depth 5

$releaseUrl = "https://firebaserules.googleapis.com/v1/projects/$ProjectId/releases/cloud.firestore?updateMask=rulesetName"
try {
    $releaseResponse = Invoke-RestMethod -Uri $releaseUrl -Method Patch -Headers $headers -Body $releasePayload
    Write-Host "  -> Successfully deployed rules to Cloud Firestore for $ProjectId!" -ForegroundColor Green
    Write-Host "`n[SUCCESS] Your new Firestore security rules are now active.`n" -ForegroundColor Green
} catch {
    Write-Error "Failed to update release: $_"
    exit 1
}
