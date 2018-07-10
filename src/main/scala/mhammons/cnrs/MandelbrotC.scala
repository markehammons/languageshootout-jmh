package mhammons.cnrs

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.{ByteBuffer, ByteOrder}
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import mhammons.cnrs

import scala.concurrent.ExecutionContext.Implicits.global
import mhammons.cnrs.c_interfaces.{Mandelbrot, SMandelbrot}
import org.graalvm.polyglot.Value
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class MandelbrotC {

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime)) @OutputTimeUnit(TimeUnit.SECONDS)
  @Warmup(time = 60, timeUnit = TimeUnit.SECONDS)
  def benchSulong(ms: MandelbrotState): Unit = {
    ms.contexts(0).mandelbrot(16000)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime)) @OutputTimeUnit(TimeUnit.SECONDS)
  @Warmup(time = 15, timeUnit = TimeUnit.SECONDS)
  def benchSulongIntegratedST(ms: MandelbrotState): Unit = {
    val wid_ht = 16000
    val context = ms.contexts(0)
    val doubleArray = Array.ofDim[Double](wid_ht)
    val m128 = context.initSSE(wid_ht)
    context.initValues(doubleArray, wid_ht)


    val header = s"P4\n$wid_ht $wid_ht\n".getBytes
    val pixels = Array.ofDim[Byte](wid_ht*wid_ht/8)
    var y = 0
    while(y < wid_ht) {
      val i0 = context.loadInit(doubleArray(y))
      val rowStart = y*wid_ht/8
      var x = 0
      while(x < wid_ht/8) {
        pixels(rowStart + x) = context.calcPixels8(x, m128)
        x += 1
      }
      y += 1
    }
    context.freem128d(m128)

    val fos = new FileOutputStream("/dev/null")
    fos.write(header)
    fos.write(pixels)
    fos.close()
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime)) @OutputTimeUnit(TimeUnit.SECONDS)
  @Warmup(time = 15, iterations=10, timeUnit = TimeUnit.SECONDS)
  def benchSulongIntegratedMT(ms: MandelbrotState): Unit = {
    val wid_ht = 16000

    val doubleArray = Array.ofDim[Double](wid_ht)
    var xy = 0
    while(xy < wid_ht) {
      doubleArray(xy) = 2.0 / wid_ht * xy - 1.0
      doubleArray(xy + 1) = 2.0 / wid_ht * (xy + 1) - 1.0
      xy += 2
    }


    val header = s"P4\n$wid_ht $wid_ht\n".getBytes
    val pixels = Array.ofDim[Byte](wid_ht, wid_ht / 8)
    val yAtomic = new AtomicInteger()
    val threads = Array.ofDim[Thread](ms.threads)
    var i = 0
    while (i < ms.threads) {
      threads(i) = new Thread(() => {
        val context = ms.contexts(i)
        context.context.enter()
        val m128 = context.initSSE(wid_ht)
        var y = yAtomic.getAndIncrement()
        while (y < wid_ht) {
          context.loadInit(doubleArray(y))
          val row = pixels(y)
          var x = 0
          while (x < wid_ht / 8) {
            row(x) = context.calcPixels8(x, m128)
            x += 1
          }
          y = yAtomic.getAndIncrement()
        }
        context.freem128d(m128)
        context.context.leave()
      })
      i += 1
    }

    i = 0
    while (i < ms.threads) {
      threads(i).start()
      i += 1
    }

    i = 0
    while (i < ms.threads) {
      threads(i).join()
      i += 1
    }
    //    (0 until wid_ht/8).par.foreach{ y =>
    //      val i = y % 8
    //      val initI = ms.contexts(i).genInitI(doubleArray(y))
    //      val rowStart = y*wid_ht/8
    //      for(x <- 0 until wid_ht/8 by 8) {
    //        ms.contexts(i).calcPixels(pixels, rowStart, x, m128s(i), initI)
    //      }
    //      ms.contexts(i).freem128d(initI)
    //    }

    val fos = new BufferedOutputStream(new FileOutputStream("/dev/null"))
    fos.write(header)

    i = 0
    while (i < wid_ht) {
      fos.write(pixels(i))
      i+=1
    }
    fos.close()
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime)) @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(time = 15, iterations=10, timeUnit = TimeUnit.SECONDS)
  def initValuesC(ms: MandelbrotState): Unit = {
    val doubleArray = Array.ofDim[Double](16000)
    ms.contexts(0).initValues(doubleArray, 16000)
  }


  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime)) @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(time = 15, iterations=10, timeUnit = TimeUnit.SECONDS)
  def initValuesJ(blackhole: Blackhole): Unit = {
    val wid_ht = 16000
    val i0 = Array.ofDim[Double](wid_ht)
    var xy = 0
    while(xy < wid_ht) {
      i0(xy) = 2.0 / wid_ht * xy - 1.0
      i0(xy + 1) = 2.0 / wid_ht * (xy + 1) - 1.0
      xy += 2
    }
    blackhole.consume(i0)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime)) @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(time = 15, iterations=10, timeUnit = TimeUnit.SECONDS)
  def initValuesN(ms: MandelbrotState, blackhole: Blackhole): Unit = {
    val wid_ht = 16000
    val values = ms.contexts(0).initValuesN(wid_ht)
    val i0 = Array.ofDim[Double](wid_ht)
    var i = 0
    println(values.hasArrayElements)
    println(values.getArraySize)
    while(i < wid_ht) {
      i0(i) = values.getArrayElement(i).asDouble()
      i += 1
    }
    ms.contexts(0).releaseValuesN(values)
    blackhole.consume(i0)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime)) @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(time = 15, iterations=10, timeUnit = TimeUnit.SECONDS)
  def stepThroughValuesJ(mandelbrotState: MandelbrotState): Unit = {
    val wid_ht = 16000
    val values = Array.ofDim[Double](16000)
    var i = 0
    while(i < wid_ht) {
      mandelbrotState.blkhole = values(i)
      i += 1
    }
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime)) @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(time = 15, iterations=10, timeUnit = TimeUnit.SECONDS)
  def stepThroughValuesC(mandelbrotState: MandelbrotState): Unit = {
    val wid_ht = 16000
    val values = Array.ofDim[Double](wid_ht)
    mandelbrotState.contexts(0).stepThroughValues.executeVoid(values, wid_ht.asInstanceOf[Object])
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime)) @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(time = 15, iterations=10, timeUnit = TimeUnit.SECONDS)
  def incrementValuesJ(blackhole: Blackhole): Unit = {
    val wid_ht = 16000
    val values = Array.ofDim[Double](16000)
    var i = 0
    while(i < wid_ht) {
      values(i) += 1
      i += 1
    }
    blackhole.consume(values)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime)) @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(time = 15, iterations=10, timeUnit = TimeUnit.SECONDS)
  def incrementValuesC(mandelbrotState: MandelbrotState): Unit = {
    val wid_ht = 16000
    val values = Array.ofDim[Double](wid_ht)
    mandelbrotState.contexts(0).incrementValues.executeVoid(values, wid_ht.asInstanceOf[Object])
  }





}

@State(Scope.Thread)
class MandelbrotState {
  var blkhole: Double = 0.0

  val threads = Runtime.getRuntime.availableProcessors

  @Setup(Level.Trial)
  def setup = {
    contexts = Array.fill(threads)(new cnrs.c_interfaces.SMandelbrot)
  }

  @TearDown(Level.Trial)
  def teardown = {
    contexts.foreach(_.close())
    contexts = null
  }

  var contexts: Array[SMandelbrot] =  null
}


