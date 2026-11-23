@echo off
rem Compila e abre o sistema (Windows). Requer Java 17 ou superior.
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -sourcepath src -d out src\app\Main.java
if errorlevel 1 pause & exit /b 1
java -cp "out;lib/*" app.Main
