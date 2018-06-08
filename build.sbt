enablePlugins(JmhPlugin)

fork := true

scalaVersion := "2.12.6"

libraryDependencies += "it.unimi.dsi" % "fastutil" % "8.1.0"

libraryDependencies += "org.graalvm" % "graal-sdk" % "1.0.0-rc2"

libraryDependencies += "com.oracle.truffle" % "truffle-api" % "1.0.0-rc2"
