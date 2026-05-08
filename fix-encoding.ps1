# Script pour corriger l'encodage UTF-8 des fichiers Java
# Remplace les caractères mal encodés par leurs équivalents corrects

$files = @(
    "src/main/java/esprit/fx/controllers/ChatbotControllerArij.java",
    "src/main/java/esprit/fx/controllers/DashboardStatsControllerArij.java",
    "src/main/java/esprit/fx/controllers/DisponibiliteController.java",
    "src/main/java/esprit/fx/controllers/QuizSanteController.java",
    "src/main/java/esprit/fx/controllers/QuizSanteControllerArij.java",
    "src/main/java/esprit/fx/services/ArticleService.java",
    "src/main/java/esprit/fx/services/ServiceDisponibilite.java",
    "src/main/java/esprit/fx/services/ServiceRendezVous.java"
)

# Table de correspondance des caractères mal encodés
$replacements = @{
    # Caractères accentués
    'Ã©' = 'é'
    'Ã¨' = 'è'
    'Ãª' = 'ê'
    'Ã§' = 'ç'
    'Ã ' = 'à'
    'Ã´' = 'ô'
    'Ã®' = 'î'
    'Ã¹' = 'ù'
    'Ã»' = 'û'
    'Ã¯' = 'ï'
    'Ã«' = 'ë'
    
    # Majuscules accentuées
    'Ã‰' = 'É'
    'Ãˆ' = 'È'
    'ÃŠ' = 'Ê'
    'Ã‡' = 'Ç'
    'Ã€' = 'À'
    'Ã"' = 'Ô'
    'ÃŽ' = 'Î'
    'Ã™' = 'Ù'
    'Ã›' = 'Û'
    
    # Symboles
    'â†'' = '→'
    'âœ"' = '✓'
    'âœ—' = '✗'
    'âš ' = '⚠'
    'â€¢' = '•'
    'aEUR¢' = '•'
    '®' = ''
    'œ' = 'œ'
    'Â' = ''
    
    # Patterns complexes
    'Ã\(c\)' = 'é'
    'Ã\(c\)' = 'è'
    'dÃ©' = 'dé'
    'reÃ§' = 'reç'
    'Ã©t' = 'ét'
    'Ã©n' = 'én'
    'Ã©r' = 'ér'
    'Ã©l' = 'él'
    'Ã©m' = 'ém'
    'Ã©v' = 'év'
    'Ã©c' = 'éc'
    'Ã©d' = 'éd'
    'Ã©p' = 'ép'
    'Ã©s' = 'és'
    'Ã©g' = 'ég'
}

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "Traitement de $file..." -ForegroundColor Cyan
        
        # Lire le contenu avec l'encodage actuel
        $content = Get-Content -Path $file -Raw -Encoding UTF8
        
        # Appliquer tous les remplacements
        foreach ($key in $replacements.Keys) {
            $content = $content -replace [regex]::Escape($key), $replacements[$key]
        }
        
        # Sauvegarder avec l'encodage UTF-8 (sans BOM)
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($file, $content, $utf8NoBom)
        
        Write-Host "  ✓ Corrigé" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Fichier introuvable: $file" -ForegroundColor Red
    }
}

Write-Host "`n✓ Correction terminée!" -ForegroundColor Green
Write-Host "Tous les fichiers ont été réencodés en UTF-8." -ForegroundColor Green
