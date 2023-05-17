# GobPie

[![build workflow status](https://github.com/goblint/GobPie/actions/workflows/build.yml/badge.svg)](https://github.com/goblint/GobPie/actions/workflows/build.yml)

The Integration of the static analyzer [Goblint](https://github.com/goblint/analyzer) into IDEs with [MagpieBridge](https://github.com/MagpieBridge/MagpieBridge).

## Installing

1. Install [Goblint](https://github.com/goblint/analyzer#installing).
2. Download [GobPie plugin](https://nightly.link/goblint/GobPie/workflows/build/master/gobpie-plugin.zip) and unzip the archive.
3. Install the extension into VSCode with `code --install-extension gobpie-0.0.3.vsix`.

When installing goblint locally (as recommended), **make sure that GobPie can find the correct version Goblint**.
This can be done in two ways:

* Setting the location of the Goblint executable used by GobPie in `gobpie.json`:
  ```yaml
  "goblintExecutable": "<installation path>/goblint"
  ```
  The *installation path* is the path to your Goblint installation.

* Activating the right opam switch before starting vscode:
  ```shell
  eval $(opam env --switch=<switch name> --set-switch)
  code .
  ```
  The *switch name* (shown in the first column of `opam switch`) is the path to your Goblint installation.

### Project prerequisites

The project must have:
1. GobPie configuration file in project root with name `gobpie.json`
2. Goblint configuration file ([see examples](https://github.com/goblint/analyzer/tree/master/conf))

#### GobPie configuration

Example GobPie configuration file `gobpie.json`:
```yaml
{
    "goblintConf": "goblint.json",
    "goblintExecutable": "/home/user/goblint/analyzer/goblint",
    "preAnalyzeCommand": ["cmake", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON", "-B", "build"],
    "showCfg": true,
    "incrementalAnalysis": false
}
```

* `goblintConf` - the relative path from the project root to the Goblint configuration file (required)
* `goblintExecutable` - the absolute or relative path to the Goblint executable (optional, default `goblint`, meaning Goblint is expected to be on `PATH`)
* `preAnalyzeCommand` - the command to run before analysing (e.g. command for building/updating the compilation database for some automation) (optional)
* `showCfg` - if the code actions for showing the function's CFGs are shown (optional, default `false`)
* `incrementalAnalyisis` - if Goblint should use incremental analysis (disabling this may, in some cases, improve the stability of Goblint) (optional, default `true`)

#### Goblint configuration

Goblint configuration file (e.g. `goblint.json`) must have the field `files` defined:

* `files` - a list of the relative paths from the project root to the files to be analysed (required)

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
2. In the repository's folder, set the correct Goblint path in `gobpie.json` or activate the right opam switch.
3. Open the project in VSCode.
