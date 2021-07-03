# Setup repo for dev on intellij

1. Clone the repo and follow `installing_live.md`
2. Importing the project in Intellij will set up the plugin code (in `src/`) through Gradle
3. Create a java module for `graalvm/`
4. Set `graalvm/src/de.hpi.swa.liveprogramming/src` as sources root
5. Open Project Settings, Libraries and add a new project library via maven: `org.graalvm.tools:lsp`
6. Now the java files should not show any errors
7. Create a new run configuration of type shell script
   1. Set the script to execute `/path/to/mx/mx build` and `gu ~/.graalvm/graalvm-ce-java11-21.2.0-dev/bin/gu install -L -f ./live-installable-java11.jar`
   2. Set working directory to the `graalvm` folder in this project
8. Run (7) everytime you have changes


### Debugging
1. Run GraalVM with `./polyglot --lsp=3000 --jvm --experimental-options --shell --log.level=ALL --verbose --vm.Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n`
2. Create a new Run-Configuration of type `Remote JVM Debug` and set the port to 8000
3. Start it and set breakpoints in the editor
