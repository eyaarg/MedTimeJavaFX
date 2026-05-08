# Script PowerShell pour corriger l'encodage UTF-8
# Utilisation: Clic droit > Exécuter avec PowerShell

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  CORRECTION DE L'ENCODAGE - TOUS LES FICHIERS JAVA" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""

# Table de correspondance
$replacements = @{
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
    'Ã‰' = 'É'
    'â†'' = '→'
    'âœ"' = '✓'
    'âœ—' = '✗'
    'âš ' = '⚠'
    'â€¢' = '•'
    'Â' = ''
    '├®' = 'é'
    '├á' = 'à'
    '├¿' = 'è'
    '├¬' = 'ê'
    '├╗' = 'û'
    'publiÃ©' = 'publié'
    'dÃ©but' = 'début'
    'reÃ§ue' = 'reçue'
    'modifiÃ©' = 'modifié'
    'supprimÃ©' = 'supprimé'
    'gÃ©nÃ©rÃ©' = 'généré'
    'dÃ©tectÃ©' = 'détecté'
    'rÃ©cupÃ©rer' = 'récupérer'
    'mÃ©decin' = 'médecin'
    'MÃ©decin' = 'Médecin'
    'disponibilitÃ©' = 'disponibilité'
    'DisponibilitÃ©' = 'Disponibilité'
    'RequÃªte' = 'Requête'
    'Ã©trangÃ¨re' = 'étrangère'
    'Ãªtre' = 'être'
    'supÃ©rieure' = 'supérieure'
    'ajoutÃ©e' = 'ajoutée'
    'modifiÃ©e' = 'modifiée'
    'supprimÃ©e' = 'supprimée'
    'liÃ©e' = 'liée'
    'liÃ©s' = 'liés'
    'utilisÃ©e' = 'utilisée'
    'Ã©lÃ©ments' = 'éléments'
    'S├®lectionner' = 'Sélectionner'
    'caract├¿res' = 'caractères'
    'suppl├®mentaires' = 'supplémentaires'
    'succ├¿s' = 'succès'
    'trouv├®e' = 'trouvée'
}

# Trouver tous les fichiers Java
$javaFiles = Get-ChildItem -Path "src" -Filter "*.java" -Recurse

Write-Host "📁 $($javaFiles.Count) fichiers Java trouvés" -ForegroundColor Yellow
Write-Host ""

$correctedCount = 0

foreach ($file in $javaFiles) {
    try {
        # Lire le contenu
        $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
        $originalContent = $content
        
        # Appliquer les remplacements
        foreach ($key in $replacements.Keys) {
            $content = $content -replace [regex]::Escape($key), $replacements[$key]
        }
        
        # Sauvegarder seulement si modifié
        if ($content -ne $originalContent) {
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
            
            Write-Host "✓ $($file.Name)" -ForegroundColor Green
            $correctedCount++
        }
    }
    catch {
        Write-Host "✗ Erreur avec $($file.Name): $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  ✓ CORRECTION TERMINÉE!" -ForegroundColor Green
Write-Host "  📊 $correctedCount fichiers corrigés sur $($javaFiles.Count) fichiers" -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Appuyez sur une touche pour fermer..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
