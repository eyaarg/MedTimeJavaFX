@echo off
REM Script de compilation Maven pour MedTimeJavaFX
REM JDK 23 (Amazon Corretto)
set JAVA_HOME=C:\Users\LENOVO\.jdks\corretto-23.0.2

REM Chemin Maven
set MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd

REM Compiler
echo Compilation du projet...
call "%MVN%" clean compile

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ Compilation réussie!
    echo.
    echo Lancer l'application avec:
    echo   "%MVN%" org.openjfx:javafx-maven-plugin:0.0.8:run
) else (
    echo.
    echo ✗ Erreur de compilation
    exit /b 1
)
