[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.14497827.svg)](https://doi.org/10.5281/zenodo.14497827)
[![SWH](https://archive.softwareheritage.org/badge/swh:1:dir:5c71cdfd2943f2f871f92241a3b4ab9b0a1197dc/)](https://archive.softwareheritage.org/swh:1:dir:5c71cdfd2943f2f871f92241a3b4ab9b0a1197dc;origin=https://github.com/celersms/AT-Emulator)

# AT-Emulator

The AT Emulator can emulate a communication device, like a modem. The AT interface can be accessed through the console, a TCP or telnet connection or a virtual COM port.

This is the public source code written in Java. Feel free to modify and use it for any commercial and non-commercial purposes. Feel free to share and redistribute this software as long as you give credit to [Victor Celer](https://www.celersms.com/org/vceler.htm).

A precompiled package and some usage examples can be found [here](https://www.celersms.com/at-emulator.htm).

## Graphical interface

The project now ships with a lightweight Swing interface that hides the AT commands required to send SMS messages.

```
$ javac src/com/celer/emul/ATGui.java src/com/celer/emul/AT.java
$ java -cp src com.celer.emul.ATGui
```

Type the destination number, compose a message and press **Enviar SMS**. The console at the bottom shows both the commands sent to the emulator and the responses returned, which is useful when learning how the AT command set behaves.

## Building with Maven

The repository now includes a Maven build that understands the existing `src` layout. The most common tasks are:

```bash
# Compile the sources (outputs to target/classes)
mvn compile

# Launch the Swing GUI directly from the compiled classes
mvn exec:java

# Produce an executable JAR with com.celer.emul.AT as the entry point
mvn package

# Run the packaged emulator from the generated JAR
java -jar target/at-emulator-1.0.2.jar
```

The Maven workflow automatically sets the manifest to launch the console emulator, defaults `mvn exec:java` to the Swing GUI, and embeds the project version in the packaged artifact. Feel free to continue using the manual `javac` commands if you prefer not to rely on Maven.
