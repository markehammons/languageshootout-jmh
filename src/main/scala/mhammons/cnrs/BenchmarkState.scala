package mhammons.cnrs

import java.io.{File, PrintStream}

import org.openjdk.jmh.annotations.{Level, Setup, TearDown}

trait BenchmarkState {
  val devNull = new File("/dev/null")
  var ps: PrintStream = null

  def doSetup(): Unit = {
    ps = new PrintStream(devNull)
  }

  def doTeardown(): Unit = {
    ps.close()
    ps = null
  }
}
