package mhammons.cnrs

import java.io.{OutputStream, PrintStream}

trait Benchable {
  def setOut(ps: PrintStream): Unit

  @throws(classOf[Exception])
  def main(args: Array[String]): Unit
}