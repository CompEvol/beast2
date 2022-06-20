REM Check whether the JRE is included
IF EXIST %~dp0\..\jre (

REM for BEAST version that includes JRE
    %~dp0\..\jre\bin\java -cp %~dp0\..\lib\launcher.jar beast.pkgmgmt.launcher.AppLauncherLauncher LogAnalyser %*

) ELSE (
REM for version that does not include JRE
    java -cp %~dp0\..\lib\launcher.jar beast.pkgmgmt.launcher.AppLauncherLauncher LogAnalyser %*
)