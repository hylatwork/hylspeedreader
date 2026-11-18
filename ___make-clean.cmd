@echo off
rem ***************************************************************************
rem * HylSpeedReader - makeClean
rem * Removes all generated build output so the next build starts from scratch.
rem * Deletes: sys-src\build, sys-src\composeApp\build, sys-src\.gradle, sys-src\.kotlin
rem *          and the built APKs in sys-src\dist (the folder itself is kept).
rem * Keeps:   sources, sys-doc\, local.properties and graphify-out\ (curated labels).
rem ***************************************************************************
setlocal
cd /D "%~dp0"

set "RC=0"

echo === HylSpeedReader: clean ===

rem --- 1) Best-effort: stop daemons and run Gradle's own clean ---------------
if exist "sys-src\gradlew.bat" (
    echo [*] Stopping Gradle daemons and cleaning via Gradle...
    pushd "sys-src"
    call gradlew.bat --stop >nul 2>&1
    call gradlew.bat clean --console=plain
    if errorlevel 1 echo [!] gradlew clean failed - continuing with manual removal
    popd
)

rem --- 2) Remove project-local caches and any leftover build output ----------
call :remove_dir "sys-src\.gradle"
call :remove_dir "sys-src\.kotlin"
call :remove_dir "sys-src\build"
call :remove_dir "sys-src\composeApp\build"

rem --- 3) Empty the distribution drop-point (keep the folder / .gitkeep) -----
call :remove_files "sys-src\dist" "*.apk"

rem --- 4) graphify-out is intentionally kept: curated labels live there -------
if exist "graphify-out\" (
    echo [=] keeping graphify-out\ - graphify labels preserved
) else (
    echo [=] graphify-out\ not present
)

if "%RC%"=="0" (
    echo [ok] Clean complete.
) else (
    echo [x] Clean finished with errors - see the messages above.
)

pause
endlocal & exit /b %RC%


:remove_dir
if exist "%~1\" (
    echo [-] removing %~1
    rd /s /q "%~1" 2>nul
    if exist "%~1\" (
        echo [!] could not remove %~1
        set "RC=1"
    )
) else (
    echo [=] %~1 not present
)
exit /b 0


:remove_files
if exist "%~1\%~2" (
    echo [-] removing %~1\%~2
    del /q "%~1\%~2" 2>nul
) else (
    echo [=] %~1\%~2 not present
)
exit /b 0
