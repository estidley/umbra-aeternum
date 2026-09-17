# Start local Postgres (Docker) and wait until it is healthy.
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Write-Error "Docker is required for local Postgres. Install Docker Desktop."
}

docker compose up -d

Write-Host "Waiting for Postgres to be healthy..."
$ready = $false
for ($i = 0; $i -lt 45; $i++) {
  docker compose exec -T postgres pg_isready -U app -d app 2>$null | Out-Null
  if ($LASTEXITCODE -eq 0) {
    $ready = $true
    break
  }
  Start-Sleep -Seconds 1
}

if (-not $ready) {
  Write-Error "Postgres did not become healthy in time. Check: docker compose logs postgres"
}

Write-Host "Postgres is ready on localhost:5432"
Write-Host ""
Write-Host "DATABASE_URL hint (copy into .env if you have not already):"
Write-Host '  DATABASE_URL="postgresql://app:app@localhost:5432/app"'
Write-Host ""
Write-Host "Next (once the product has Prisma):"
Write-Host "  npx prisma generate"
Write-Host "  npx prisma migrate deploy"
Write-Host "  npx prisma db seed"
Write-Host "  npm run dev"
