call mvn clean package -f "../pom.xml"
call vsce package
wsl -- bash ./install_and_test.sh
