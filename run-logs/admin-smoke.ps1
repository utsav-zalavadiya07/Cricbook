$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/api/v1'
$verify = Invoke-RestMethod -Method Post -Uri "$base/admin/auth/verify-otp" -ContentType 'application/json' -Body (@{ email='admin@cricbook.com'; otp='123456' } | ConvertTo-Json)
$token = $verify.accessToken
if (-not $token) { $token = $verify.token }
if (-not $token -and $verify.data) { $token = $verify.data.accessToken; if (-not $token) { $token = $verify.data.token } }
if (-not $token) { throw "Token missing: $($verify | ConvertTo-Json -Depth 5)" }
$headers = @{ Authorization = "Bearer $token" }
$service = Get-Content 'frontend\modules\admin\services\adminService.ts'
$paths = New-Object System.Collections.Generic.List[string]
foreach ($line in $service) {
  if ($line -match 'api\.get(?:<[^\)]*>)?\("([^"]+)"') {
    $p = $Matches[1]
    if ($p -notmatch '\$|\{') { $paths.Add($p) }
  }
}
$paths = $paths | Where-Object { $_ -like '/admin/*' -or $_ -like '/platform/*' -or $_ -eq '/campaigns' } | Sort-Object -Unique
$results = @()
foreach ($path in $paths) {
  $url = "$base$path"
  try {
    $response = Invoke-WebRequest -Method Get -Uri $url -Headers $headers -TimeoutSec 20 -UseBasicParsing
    $results += [pscustomobject]@{ Status = [int]$response.StatusCode; Path = $path; Error = '' }
  } catch [System.Net.WebException] {
    $status = 0
    $body = ''
    if ($_.Exception.Response) {
      $status = [int]$_.Exception.Response.StatusCode
      try {
        $stream = $_.Exception.Response.GetResponseStream()
        if ($stream) { $body = (New-Object IO.StreamReader($stream)).ReadToEnd() }
      } catch {}
    }
    $results += [pscustomobject]@{ Status = $status; Path = $path; Error = ($_.Exception.Message + ' ' + $body).Trim() }
  } catch {
    $results += [pscustomobject]@{ Status = 0; Path = $path; Error = $_.Exception.Message }
  }
}
$failures = $results | Where-Object { $_.Status -ge 400 -or $_.Status -eq 0 }
$results | Sort-Object Status,Path | Format-Table -AutoSize
Write-Output "SUMMARY: $($results.Count - $failures.Count)/$($results.Count) passed; failures=$($failures.Count)"
if ($failures.Count -gt 0) { Write-Output 'FAILURES:'; $failures | Format-List }
