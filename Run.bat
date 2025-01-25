@echo off
call .\mvn-scripts\windows\CompileToRepository.bat
call .\mvn-scripts\windows\Mvn-Exec-Windows.bat
PAUSE