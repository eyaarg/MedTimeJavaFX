@echo off
echo ========================================
echo Recompilation du projet MedTimeJavaFX
echo ========================================
echo.

echo Nettoyage du projet...
call mvn clean

echo.
echo Compilation du projet...
call mvn compile -DskipTests

echo.
echo ========================================
echo Recompilation terminee!
echo ========================================
echo.
echo Vous pouvez maintenant relancer l'application.
echo.
pause
