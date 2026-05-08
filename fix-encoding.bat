@echo off
chcp 65001 >nul
echo Correction de l'encodage des fichiers Java...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command "& {$files = @('src/main/java/esprit/fx/controllers/ChatbotControllerArij.java', 'src/main/java/esprit/fx/controllers/DashboardStatsControllerArij.java', 'src/main/java/esprit/fx/controllers/DisponibiliteController.java', 'src/main/java/esprit/fx/controllers/QuizSanteController.java', 'src/main/java/esprit/fx/controllers/QuizSanteControllerArij.java', 'src/main/java/esprit/fx/services/ArticleService.java', 'src/main/java/esprit/fx/services/ServiceDisponibilite.java', 'src/main/java/esprit/fx/services/ServiceRendezVous.java'); foreach ($file in $files) { if (Test-Path $file) { Write-Host \"Traitement de $file...\" -ForegroundColor Cyan; $content = Get-Content -Path $file -Raw -Encoding UTF8; $content = $content -replace 'Ã©', 'é' -replace 'Ã¨', 'è' -replace 'Ãª', 'ê' -replace 'Ã§', 'ç' -replace 'Ã ', 'à' -replace 'Ã´', 'ô' -replace 'Ã®', 'î' -replace 'Ã¹', 'ù' -replace 'Ã»', 'û' -replace 'Ã¯', 'ï' -replace 'Ã«', 'ë' -replace 'Ã‰', 'É' -replace 'Ãˆ', 'È' -replace 'ÃŠ', 'Ê' -replace 'Ã‡', 'Ç' -replace 'Ã€', 'À' -replace 'Ã"', 'Ô' -replace 'ÃŽ', 'Î' -replace 'Ã™', 'Ù' -replace 'Ã›', 'Û' -replace 'â†'', '→' -replace 'âœ"', '✓' -replace 'âœ—', '✗' -replace 'âš ', '⚠' -replace 'â€¢', '•' -replace 'aEUR¢', '•' -replace 'Â', '' -replace 'Ã\(c\)', 'é'; $utf8NoBom = New-Object System.Text.UTF8Encoding \$false; [System.IO.File]::WriteAllText(\$file, \$content, \$utf8NoBom); Write-Host \"  ✓ Corrigé\" -ForegroundColor Green } else { Write-Host \"  ✗ Fichier introuvable: \$file\" -ForegroundColor Red } }; Write-Host \"`n✓ Correction terminée!\" -ForegroundColor Green }"

echo.
echo Terminé!
pause
