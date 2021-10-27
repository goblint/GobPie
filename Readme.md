# GobPie

[![build workflow status](https://github.com/goblint/GobPie/actions/workflows/build.yml/badge.svg)](https://github.com/goblint/GobPie/actions/workflows/build.yml)

The Integration of analyzer Goblint (Command Line Tool) into IDEs with MagpieBridge

## Installing

1. [Install Goblint](https://github.com/goblint/analyzer#installing)
2. Install GobPie extension into VSCode following these steps:
   * Clone this repository
   * In the repository's folder run these commands:
        ~~~
        mvn install
        cd vscode
        npm install
        npm install -g vsce
        vsce package
        code --install-extension goblintanalyzer-0.0.1.vsix
        ~~~

To use the extension on a project, **make sure that the right opam switch is activated in the project's folder**.

* To activate the right opam switch, use command: `eval $(opam env --switch=` \<insert-switch-name\> ` --set-switch)` <br>
* To find out the switch name, run command `opam switch`, the name is in the first column. 


### To test the extension

1. Clone the [demoproject](https://github.com/karoliineh/GoblintAnalyzer-DemoProject)
2. In the repository's folder, activate the right opam switch.
3. Open the project in VSCode.
