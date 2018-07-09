package mhammons.cnrs.c_interfaces

import org.graalvm.polyglot.{Context, Source}

abstract class Sulong(source: Source) {
  val context = Context.newBuilder().allowNativeAccess(true).build()
  val lib = context.eval(source)

  context.initialize("llvm")
}
