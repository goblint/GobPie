# GobPie

[![build workflow status](https://github.com/goblint/GobPie/actions/workflows/build.yml/badge.svg)](https://github.com/goblint/GobPie/actions/workflows/build.yml)

The Integration of analyzer Goblint (Command Line Tool) into IDEs with MagpieBridge

## Installing

1. Install [Goblint](https://github.com/goblint/analyzer#installing).
2. Download [GobPie](https://nightly.link/goblint/GobPie/workflows/build/master/plugin.zip) and unzip the archive.
3. Install the extension into VSCode with `code --install-extension goblintanalyzer-0.0.1.vsix`.

When installing goblint locally (as recommended), **make sure that the right opam switch is activated when starting vscode**:
```
eval $(opam env --switch=<switch name> --set-switch)
code .
```
The *switch name* (shown in the first column of `opam switch`) is the path to the goblint installation.


## Developing

To build this extension, run the commands:

~~~
mvn install
cd vscode
npm install
npm install -g vsce
vsce package
~~~


## To test the extension

1. Clone the [demoproject](https://github.com/karoliineh/GoblintAnalyzer-DemoProject)
2. In the repository's folder, activate the right opam switch.
3. Open the project in VSCode.
