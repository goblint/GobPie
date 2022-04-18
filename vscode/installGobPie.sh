mvn clean -f "../../GobPie/pom.xml"
mvn install -f "../../GobPie/pom.xml"
echo y | vsce package
code --install-extension gobpie-0.0.2.vsix