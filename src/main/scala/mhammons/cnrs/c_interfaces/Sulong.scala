package mhammons.cnrs.c_interfaces

import java.io.File

import org.graalvm.polyglot.{Context, Source}

abstract class Sulong(sourceFile: File) {
  val source = Source.newBuilder("llvm", sourceFile).build()
  val context = Context.newBuilder().allowNativeAccess(true).build()
  val lib = context.eval(source)

  context.initialize("llvm")
}
