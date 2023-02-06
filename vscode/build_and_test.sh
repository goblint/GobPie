mvn clean package -f "../pom.xml"
vsce package
bash ./install_and_test.sh
