call mvn clean package -f "../pom.xml"
if %errorlevel% neq 0 exit /b %errorlevel%
call vsce package
if %errorlevel% neq 0 exit /b %errorlevel%
wsl -- bash ./install_and_test.sh
