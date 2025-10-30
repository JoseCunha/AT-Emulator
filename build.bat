@echo off
SETLOCAL
REM === CONFIG BEGIN =========================================

REM If you have Proguard installed, set the jar location below
SET PROGUARD=C:\Tools\proguard\lib\proguard.jar
REM For JDK 9+ you must also provide a compatible runtime image or jar for
REM ProGuard to resolve platform classes (for example a jlink image or
REM classpath jar). Leave empty to skip the optimization step.
SET PROGUARD_LIBRARIES=

REM === CONFIG END ===========================================
TITLE Building AT Emulator...
PUSHD "%~dp0"

REM Resolve the JDK path
SET JMINVER=11
SET JDK=%JDK_HOME%
IF EXIST "%JDK%\bin\javac.exe" GOTO JDKFOUND
SET JDK=%JAVA_HOME%
IF EXIST "%JDK%\bin\javac.exe" GOTO JDKFOUND
FOR /f "tokens=2*" %%i IN ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit" /s 2^>nul ^| find "JavaHome"') DO SET JDK=%%j
IF EXIST "%JDK%\bin\javac.exe" GOTO JDKFOUND
FOR /f "tokens=2*" %%i IN ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\JavaSoft\Java Development Kit" /s 2^>nul ^| find "JavaHome"') DO SET JDK=%%j
IF NOT EXIST "%JDK%\bin\javac.exe" GOTO JDKNOTFOUND
:JDKFOUND

REM Get the AT Emulator version
SET VER=0.0.0
FOR /F "tokens=*" %%i IN (src\VERSION.txt) DO SET VER=%%i

REM Compile the AT Emulator
rd /s /q classes >nul 2>nul
mkdir classes\emul 2>nul
"%JDK%\bin\javac" --release %JMINVER% -d classes\emul -g:lines -classpath src src\com\celer\emul\*.java
IF %ERRORLEVEL% NEQ 0 GOTO EXIT

REM Update the build number and generate the Manifest
SET BLD=0
FOR /F "tokens=*" %%i IN (src\Emul.bld) DO SET BLD=%%i
SET /A BLD=BLD+1
(ECHO %BLD%) >src\Emul.bld
ECHO New build number: %BLD%
(
ECHO Manifest-Version: 1.0
ECHO DeliveryID: AT_Emulator_%VER%b%BLD%
ECHO Created-By: CelerSMS
ECHO Main-Class: com.celer.emul.AT
ECHO Copyright: CelerSMS, 2018-2025
ECHO.
) >classes\Emul.MF
mkdir _rel 2>nul

REM Pack the JAR (optionally optimize with Proguard)
IF NOT EXIST "%PROGUARD%" GOTO NOPROGUARD
IF "%PROGUARD_LIBRARIES%"=="" GOTO NOPROGUARD
"%JDK%\bin\jar" cmf classes\Emul.MF emul_in.jar -C classes\emul .
IF %ERRORLEVEL% NEQ 0 GOTO EXIT
"%JDK%\bin\java" -jar %PROGUARD% -injars emul_in.jar -outjars _rel\AT.jar -libraryjars "%PROGUARD_LIBRARIES%" @src\Emul.pro
IF %ERRORLEVEL% NEQ 0 GOTO EXIT
del emul_in.jar /q >nul 2>nul
GOTO BLDOK
:NOPROGUARD
"%JDK%\bin\jar" cmf classes\Emul.MF _rel\AT.jar -C classes\emul .
IF %ERRORLEVEL% NEQ 0 GOTO EXIT
:BLDOK
GOTO EXIT

:JDKNOTFOUND
ECHO JDK not found. If you have JDK %JMINVER% or later installed, set JDK_HOME environment variable to the JDK installation directory.
GOTO EXIT
:EXIT
pause
POPD
ENDLOCAL
@echo on
