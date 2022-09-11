@echo off

%~dp0\..\jre\bin\java -cp %~dp0\..\lib\launcher.jar beast.pkgmgmt.launcher.AppLauncherLauncher PackageHealthChecker %*
