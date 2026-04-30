@echo off
REM Script pour lancer l'application JavaFX
REM JDK 23 (Amazon Corretto)
set JAVA_HOME=C:\Users\LENOVO\.jdks\corretto-23.0.2

REM Chemin Maven
set MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd

REM Lancer l'application
echo Lancement de MedTimeJavaFX...
call "%MVN%" org.openjfx:javafx-maven-plugin:0.0.8:run
