@echo off

REM Check whether the JRE is included
IF EXIST %~dp0\..\jre (

REM for BEAST version that includes JRE
    "%~dp0\..\jre\bin\java" -Dfile.encoding=UTF-8 -cp "%~dp0\..\lib\launcher.jar" beast.pkgmgmt.launcher.BeastLauncher %*

) ELSE (
REM for version that does not include JRE
    java -Dfile.encoding=UTF-8 -cp "%~dp0\..\lib\launcher.jar" beast.pkgmgmt.launcher.BeastLauncher %*
)