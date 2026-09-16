@ECHO OFF
REM ---------------------------------------------------------------------------
REM Maven Wrapper for the EMG Kiro sandbox (Windows).
REM Downloads Apache Maven on first use into %USERPROFILE%\.m2\wrapper.
REM Uses the system proxy with your Windows credentials.
REM Requires a JDK 21.
REM ---------------------------------------------------------------------------
SETLOCAL

SET MAVEN_VERSION=3.9.9
SET WRAPPER_HOME=%USERPROFILE%\.m2\wrapper
SET MAVEN_HOME=%WRAPPER_HOME%\apache-maven-%MAVEN_VERSION%
SET DIST_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip

IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
    ECHO Apache Maven %MAVEN_VERSION% not found locally.
    ECHO Downloading it once into %MAVEN_HOME% ...
    IF NOT EXIST "%WRAPPER_HOME%" MKDIR "%WRAPPER_HOME%"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $wc=New-Object Net.WebClient; $p=[Net.WebRequest]::GetSystemWebProxy(); $p.Credentials=[Net.CredentialCache]::DefaultCredentials; $wc.Proxy=$p; $zip=Join-Path '%WRAPPER_HOME%' 'maven.zip'; $wc.DownloadFile('%DIST_URL%',$zip); Add-Type -AssemblyName System.IO.Compression.FileSystem; [IO.Compression.ZipFile]::ExtractToDirectory($zip,'%WRAPPER_HOME%'); Remove-Item $zip"
    IF ERRORLEVEL 1 (
        ECHO.
        ECHO ERROR: download failed.
        ECHO Install Maven manually, or see the manual steps in the sandbox README.
        EXIT /B 1
    )
    ECHO Done.
)

CALL "%MAVEN_HOME%\bin\mvn.cmd" %*
