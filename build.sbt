enablePlugins(JmhPlugin)

fork := true

scalaVersion := "2.12.6"

libraryDependencies += "it.unimi.dsi" % "fastutil" % "8.1.0"

libraryDependencies += "org.graalvm" % "graal-sdk" % "1.0.0-rc3"

val nativeSources = List(
  "src/main/C/Nbody.c",
  "src/main/C/Mandelbrot.c"
)

val nativeCompile = taskKey[Int]("Compiles the native library")

nativeCompile := {
  import sys.process._
  (for(source <- nativeSources) yield {
    val sourceFile = new File(source)
    (s"gcc -shared -fPIC -o target/${sourceFile.getName.stripSuffix(".c")}.so $source") !
  }).find(_ != 0).getOrElse(0)
}

val irCompile = taskKey[Int]("Compiles the IR code")




irCompile := {
  import sys.process._
  (for(source <- nativeSources) yield {
    val sourceFile = new File(source)
    (s"clang -c -O1 -emit-llvm -o target/${sourceFile.getName.stripSuffix(".c")}.bc $source") !
  }).find(_ != 0).getOrElse(0)
}