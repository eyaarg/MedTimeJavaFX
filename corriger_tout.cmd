@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo.
echo ========================================================================
echo   CORRECTION AUTOMATIQUE DE TOUS LES SYMBOLES MAL ENCODES
echo ========================================================================
echo.

set "count=0"

for /r "src" %%f in (*.java) do (
    echo Traitement: %%~nxf
    
    REM Créer un fichier temporaire
    set "tempfile=%%f.tmp"
    
    REM Lire et corriger le fichier
    powershell -NoProfile -Command ^
        "$content = Get-Content -Path '%%f' -Raw -Encoding UTF8; ^
         $content = $content -replace 'Ã©', 'é' -replace 'Ã¨', 'è' -replace 'Ãª', 'ê' -replace 'Ã§', 'ç' -replace 'Ã ', 'à' -replace 'Ã´', 'ô' -replace 'Ã®', 'î' -replace 'Ã¹', 'ù' -replace 'Ã»', 'û' -replace 'Ã¯', 'ï' -replace 'Ã«', 'ë' -replace 'Ã‰', 'É' -replace 'â†'', '→' -replace 'âœ"', '✓' -replace 'âœ—', '✗' -replace 'âš ', '⚠' -replace 'â€¢', '•' -replace 'Â', '' -replace '├®', 'é' -replace '├á', 'à' -replace '├¿', 'è' -replace '├¬', 'ê' -replace '├╗', 'û' -replace 'ÔÜá', '⚠️' -replace 'Ô×ò', '🆕' -replace 'Ô£Å', '✏️' -replace 'ÔÇö', '—' -replace 'publiÃ©', 'publié' -replace 'dÃ©but', 'début' -replace 'reÃ§ue', 'reçue' -replace 'modifiÃ©', 'modifié' -replace 'supprimÃ©', 'supprimé' -replace 'gÃ©nÃ©rÃ©', 'généré' -replace 'dÃ©tectÃ©', 'détecté' -replace 'rÃ©cupÃ©rer', 'récupérer' -replace 'mÃ©decin', 'médecin' -replace 'MÃ©decin', 'Médecin' -replace 'disponibilitÃ©', 'disponibilité' -replace 'DisponibilitÃ©', 'Disponibilité' -replace 'RequÃªte', 'Requête' -replace 'Ã©trangÃ¨re', 'étrangère' -replace 'Ãªtre', 'être' -replace 'supÃ©rieure', 'supérieure' -replace 'ajoutÃ©e', 'ajoutée' -replace 'modifiÃ©e', 'modifiée' -replace 'supprimÃ©e', 'supprimée' -replace 'liÃ©e', 'liée' -replace 'liÃ©s', 'liés' -replace 'utilisÃ©e', 'utilisée' -replace 'Ã©lÃ©ments', 'éléments' -replace 'S├®lectionner', 'Sélectionner' -replace 'caract├¿res', 'caractères' -replace 'suppl├®mentaires', 'supplémentaires' -replace 'succ├¿s', 'succès' -replace 'trouv├®e', 'trouvée' -replace 'Ã\(c\)', 'é'; ^
         $utf8NoBom = New-Object System.Text.UTF8Encoding $false; ^
         [System.IO.File]::WriteAllText('%%f', $content, $utf8NoBom)"
    
    if !errorlevel! equ 0 (
        set /a count+=1
        echo   [OK] Corrige
    ) else (
        echo   [ERREUR]
    )
    echo.
)

echo.
echo ========================================================================
echo   TERMINE! %count% fichiers traites
echo ========================================================================
echo.
pause
