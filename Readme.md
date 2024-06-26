# GobPie

[![build workflow status](https://github.com/goblint/GobPie/actions/workflows/build.yml/badge.svg)](https://github.com/goblint/GobPie/actions/workflows/build.yml)
[![Coverage Status](https://coveralls.io/repos/github/goblint/GobPie/badge.svg?branch=master)](https://coveralls.io/github/goblint/GobPie?branch=master)

The Integration of the static analyzer [Goblint](https://github.com/goblint/analyzer) into IDEs with [MagpieBridge](https://github.com/MagpieBridge/MagpieBridge).

## Installing

1. Install [Goblint](https://github.com/goblint/analyzer#installing).
2. Download [GobPie plugin](https://nightly.link/goblint/GobPie/workflows/build/master/gobpie-plugin.zip) and unzip the archive.
3. Install the extension into VSCode with `code --install-extension gobpie-0.0.4.vsix`.

When installing goblint locally (as recommended), **make sure that GobPie can find the correct version of Goblint**.
This can be done in two ways:

* Setting the location of the Goblint executable used by GobPie in `gobpie.json`:
  ```yaml
  "goblintExecutable": "<installation path>/goblint"
  ```
  The *installation path* is the path to your Goblint installation.

* Activating the right opam switch before starting VS Code:
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
    "abstractDebugging": true,
    "showCfg": true,
    "incrementalAnalysis": false
}
```

* `goblintConf` - the relative path from the project root to the Goblint configuration file (required)
* `goblintExecutable` - the absolute or relative path to the Goblint executable (optional, default `goblint`, meaning Goblint is expected to be on `PATH`)
* `preAnalyzeCommand` - the command to run before analysing (e.g. command for building/updating the compilation database for some automation) (optional)
* `abstractDebugging` - if [abstract debugging](#abstract-debugging) is enabled (this automatically enables ARG generation) (optional, default `false`)
* `showCfg` - if the code actions for showing the function's CFGs are shown (optional, default `false`)
* `incrementalAnalyisis` - if Goblint should use incremental analysis (disabling this may, in some cases, improve the stability of Goblint) (optional, default `true`)
* `explodeGroupWarnings` - if Goblint's group warnings are "exploded", meaning that the group warning is shown at each location of an individual warning within it, or if they are not "exploded", meaning the group warning is shown only at a single defined location.
  Currently, it only affects data race warnings, so if enabled, the data race warning will be shown at the location of each of the accesses, and if disabled, the warning will be shown only at the variable that is accessed. (optional, default `true`).

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

## Abstract debugging

GobPie includes a special debugger called an **abstract debugger**, that uses the results of Goblint's analysis to emulate a standard debugger, but operates on abstract states instead of an actual running program.

Once GobPie is installed and configured (see previous two sections), the debugger can be started by simply selecting "C (GobPie Abstract Debugger)" from the Run and Debug panel in VS Code and starting the debugger as normal.  
Note: Abstract debugging is disabled by default. It must be explicitly enabled in `gobpie.json` before starting GobPie.

The debugger supports breakpoints, including conditional breakpoints, shows the call stack and values of variables, allows interactive evaluation of expressions and setting watch expressions, and supports most standard stepping operations.

In general, the abstract debugger works analogously to a normal debugger, but instead of stepping through a running program, it steps through the program's ARG generated by Goblint during analysis.
Values of variables and expressions are obtained from the Goblint base analysis and are represented using the base analysis abstract domain.
The function call stack is constructed by traversing the ARG from the current node to the program entry point.
In case of multiple possible call stacks, all possible callers at the location where call stacks diverge are shown. To view the call stack of one possible caller, the restart frame operation can be used to restart the frame of the desired caller, which moves the active location to the start of the selected caller's frame.

When there are multiple possible ARG nodes at the location of a breakpoint, then all possible ARG nodes are shown at the same time as threads.
When a step is made in one thread, an equivalent step is made in all other threads. An equivalent step is one that leads to the same CFG node. This means that all threads are synchronized such that stepping to an ARG node in one thread ensures that all threads are at an ARG node with the same corresponding CFG node.

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
