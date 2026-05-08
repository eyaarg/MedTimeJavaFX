# Read the file
$content = [System.IO.File]::ReadAllText("src/main/java/esprit/fx/services/ServiceDisponibilite.java", [System.Text.Encoding]::UTF8)

# Apply replacements
$content = $content -replace 'Ã©', 'é'
$content = $content -replace 'Ã¨', 'è'
$content = $content -replace 'Ãª', 'ê'
$content = $content -replace 'Ã§', 'ç'
$content = $content -replace 'Ã ', 'à'
$content = $content -replace 'Ã´', 'ô'
$content = $content -replace 'Ã»', 'û'
$content = $content -replace 'Ã®', 'î'
$content = $content -replace 'Ã¯', 'ï'
$content = $content -replace 'Ã¹', 'ù'
$content = $content -replace 'Ã‰', 'É'
$content = $content -replace 'Ã€', 'À'
$content = $content -replace 'â†'', '→'
$content = $content -replace 'âœ"', '✓'
$content = $content -replace 'âœ—', '✗'
$content = $content -replace 'âš ', '⚠'

# Write back with UTF-8 encoding (no BOM)
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText("src/main/java/esprit/fx/services/ServiceDisponibilite.java", $content, $utf8NoBom)

Write-Host "✓ Fixed all encoding issues in ServiceDisponibilite.java"
