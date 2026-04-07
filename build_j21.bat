@echo off
set JAVA_HOME=C:\Users\YTGBS\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "d:\Games\Minecraft\Self Made mods\wynnrolls"
call gradlew.bat build
if %ERRORLEVEL% equ 0 (
    copy /Y "build\libs\wynnrolls-1.0.0.jar" "C:\Users\YTGBS\curseforge\minecraft\Instances\Wynn Fruma\mods\wynnrolls-1.0.0.jar"
    echo Mod kopiert.
) else (
    echo BUILD FEHLGESCHLAGEN.
)
