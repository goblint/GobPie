### The Integration of analyzer Goblint (Command Line Tool) into IDEs with MagpieBridge

[![build workflow status](https://github.com/goblint/GobPie/actions/workflows/build.yml/badge.svg)](https://github.com/goblint/GobPie/actions/workflows/build.yml)

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


### To test the extension

1. Demoproject: https://github.com/karoliineh/GoblintAnalyzer-DemoProject
