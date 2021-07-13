The Integration of analyzer Goblint (Command Line Tool) into IDEs with MagpieBridge

To install the tool into VSCode:

1. Clone this repository
2. Run these commands:
~~~
mvn install
cd vscode
npm install
npm install -g vsce
vsce package
code --install-extension goblintanalyzer-0.0.1.vsix
~~~
