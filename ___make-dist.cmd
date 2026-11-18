@echo off
rem ***************************************************************************
rem * HylSpeedReader - makeDist
rem * Builds the project and produces the distribution artifacts.
rem * Runs:   sys-src\gradlew.bat :composeApp:assembleRelease
rem *         (assembleRelease is finalized by the copyReleaseApkToDist task,
rem *          which copies the built release APK into sys-src\dist)
rem * Output: sys-src\composeApp\build\outputs\apk\release\*.apk
rem *         sys-src\dist\*.apk  (the distribution drop-point)
rem ***************************************************************************
setlocal
cd /D "%~dp0"

set "RC=0"

echo === HylSpeedReader: make dist ===

rem --- 1) Locate the Gradle wrapper -------------------------------------------
if not exist "sys-src\gradlew.bat" (
    echo [x] sys-src\gradlew.bat not found - cannot build.
    set "RC=1"
    goto :done
)

rem --- 2) Build the release (produces the APK and populates sys-src\dist) ------
echo [*] Building release (:composeApp:assembleRelease)...
pushd "sys-src"
call gradlew.bat :composeApp:assembleRelease --console=plain
set "BUILD_RC=%ERRORLEVEL%"
popd
if not "%BUILD_RC%"=="0" (
    echo [x] Release build failed with exit code %BUILD_RC%.
    set "RC=1"
    goto :done
)
echo [ok] Release build succeeded.

rem --- 3) Verify the distribution artifacts -----------------------------------
call :count_apks "sys-src\dist"
if "%APK_COUNT%"=="0" (
    echo [!] No APK found in sys-src\dist - copyReleaseApkToDist may have failed.
    set "RC=1"
) else (
    echo [ok] Distribution artifacts in sys-src\dist:
    for %%F in ("sys-src\dist\*.apk") do echo     %%~nxF  ^(%%~zF bytes^)
)

:done
if "%RC%"=="0" (
    echo [ok] make dist complete.
) else (
    echo [x] make dist finished with errors - see the messages above.
)

pause
endlocal & exit /b %RC%


:count_apks
set "APK_COUNT=0"
if exist "%~1\*.apk" for %%F in ("%~1\*.apk") do set /a APK_COUNT+=1
exit /b 0
