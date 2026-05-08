@echo off
chcp 65001 >nul
echo.
echo ========================================================================
echo   CORRECTION DE L'ENCODAGE - TOUS LES FICHIERS JAVA
echo ========================================================================
echo.
echo Ce script va corriger l'encodage UTF-8 de tous les fichiers Java
echo du projet (vos fichiers + ceux de vos collegues).
echo.
echo Appuyez sur une touche pour continuer...
pause >nul

echo.
echo Correction en cours...
echo.

python fix_all_encoding.py

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================================================
    echo   ERREUR: Python n'est pas installe ou n'est pas dans le PATH
    echo ========================================================================
    echo.
    echo Solutions:
    echo   1. Installez Python depuis https://www.python.org/downloads/
    echo   2. OU utilisez la methode manuelle dans votre editeur de code
    echo.
) else (
    echo.
    echo ========================================================================
    echo   SUCCES! Tous les fichiers ont ete corriges
    echo ========================================================================
    echo.
)

echo.
echo Appuyez sur une touche pour fermer...
pause >nul
