## Installing GraalVM Live Extension (on unix)

1. Clone `graalvm/mx`
2. Clone this repo
3. Install GraalVM for Java 11
4. Download the latest release of `graalvm/labs-openjdk-11` and unpack it
5. Touch `graalvm/mx.live-programming/env` and insert: ```DISABLE_INSTALLABLES=false
   JAVA_HOME=/path/to/labsjdk-ce-your-version```
6. Go into `graalvm` and run `path/to/mx/mx build`
7. After building there should be a `live-installable-java11.jar`
8. Run `/path/to/graalvm/bin/gu install -f -L /path/to/live-installable-java11.jar` (Here `graalvm` is the actual graalvm instance, not the folder in this repo)
