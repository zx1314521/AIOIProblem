@echo off
setlocal
if not exist ".mvn\wrapper\maven-wrapper.jar" (
  where.exe mvn.cmd >nul 2>nul
  if not errorlevel 1 (
    call mvn.cmd %*
    exit /b
  )
  where.exe mvn.exe >nul 2>nul
  if not errorlevel 1 (
    call mvn.exe %*
    exit /b
  )
  echo Maven wrapper jar is missing and system Maven was not found. Install JDK 21 and Maven, then run mvnw.cmd again.
  exit /b 1
)
java -jar .mvn\wrapper\maven-wrapper.jar %*
