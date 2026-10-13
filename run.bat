@echo off
REM Compiles and runs the app on Windows.
REM Needs: JDK 17 or newer, and the MySQL Connector/J .jar inside the lib folder.
cd /d %~dp0
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out -cp "lib/*" @sources.txt
if errorlevel 1 goto :end
java -cp "out;lib/*" ntm.Main
:end
del sources.txt
pause
