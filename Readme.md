# GobPie

[![build workflow status](https://github.com/goblint/GobPie/actions/workflows/build.yml/badge.svg)](https://github.com/goblint/GobPie/actions/workflows/build.yml)

The Integration of the static analyzer [Goblint](https://github.com/goblint/analyzer) into IDEs with [MagpieBridge](https://github.com/MagpieBridge/MagpieBridge).

## Installing

1. Install [Goblint](https://github.com/goblint/analyzer#installing).
2. Download [GobPie plugin](https://nightly.link/goblint/GobPie/workflows/build/master/gobpie-plugin.zip) and unzip the archive.
3. Install the extension into VSCode with `code --install-extension gobpie-0.0.3.vsix`.

When installing goblint locally (as recommended), **make sure that the right opam switch is activated when starting vscode**:
```
eval $(opam env --switch=<switch name> --set-switch)
code .
```
The *switch name* (shown in the first column of `opam switch`) is the path to the goblint installation.

### Project prerequisites

The project must have:
1. GobPie configuration file in project root with name "`gobpie.json`"
2. Goblint configuration file ([see examples](https://github.com/goblint/analyzer/tree/master/conf))

#### GobPie configuration

Example GobPie configuration file `gobpie.json`:
```
{
    "goblintConf" : "goblint.json",
    "preAnalyzeCommand" : ["cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"],
    "showCfg": true
}
```

* `goblintConf` - the relative path from project root to the goblint configuration file (required)
* `preAnalyzeCommand` - the command to run before analysing (e.g. command for building/updating the compilation database for some automation) (optional)
* `showCfg` - if the code actions for showing function's CFGs are shown

#### Goblint configuration

Goblint configuration file (e.g. `goblint.json`) must have field `files` defined:

* `files` - a list of the relative paths from project root to the files to be analysed (required)

Example values for `files`:
* analyse files according to a compilation database:
  * `["."]`  (current directory should have the database)
  * `["./build"]` (build directory should have the database)
  * `["./build/compile_commands.json"]` (direct path to the database, not its directory)
* analyse specified file(s) from the project:
  * `["./01-assert.c"]` (single file for analysis without database)
  * `["./01-assert.c", "extra.c"]` (multiple files for analysis without database)

## Developing

Make sure the following are installed: `JDK 17`, `mvn`, `npm`, `nodejs`, `@vscode/vsce`.

To build this extension, run the commands:

~~~
mvn install
cd vscode
npm install
npm install -g vsce
vsce package
~~~


## To test the extension

1. Clone the [demo project](https://github.com/karoliineh/GoblintAnalyzer-DemoProject)
2. In the repository's folder, activate the right opam switch.
3. Open the project in VSCode.
