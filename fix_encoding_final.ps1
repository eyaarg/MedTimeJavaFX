$file = "src/main/java/esprit/fx/services/ServiceDisponibilite.java"
$content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)

Write-Host "Fixing encoding issues in ServiceDisponibilite.java..."

$content = $content.Replace('Ã©', 'é')
$content = $content.Replace('Ã¨', 'è')
$content = $content.Replace('Ãª', 'ê')
$content = $content.Replace('Ã§', 'ç')
$content = $content.Replace('Ã ', 'à')
$content = $content.Replace('Ã´', 'ô')
$content = $content.Replace('Ã»', 'û')
$content = $content.Replace('Ã®', 'î')
$content = $content.Replace('Ã¯', 'ï')
$content = $content.Replace('Ã¹', 'ù')
$content = $content.Replace('Ã‰', 'É')
$content = $content.Replace('Ã€', 'À')
$content = $content.Replace('â†'', '→')
$content = $content.Replace('âœ"', '✓')
$content = $content.Replace('âœ—', '✗')
$content = $content.Replace('âš ', '⚠')

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($file, $content, $utf8NoBom)

Write-Host "✓ Encoding issues fixed in ServiceDisponibilite.java"
