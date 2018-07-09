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
      val i0 = doubleArray(y)
      val rowStart = y*wid_ht/8
      var x = 0
      while(x < wid_ht/8) {
        pixels(rowStart + x) = context.calcPixels8(x, m128, i0)
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
    ms.contexts(0).initValues(doubleArray, wid_ht)

    val header = s"P4\n$wid_ht $wid_ht\n".getBytes
    val pixels = Array.ofDim[Byte](wid_ht, wid_ht / 8)
    val yAtomic = new AtomicInteger()
    val threads = Array.ofDim[Thread](ms.threads)
    var i = 0
    while (i < ms.threads) {
      threads(i) = new Thread(() => {
        val context = ms.contexts(i)
        val m128 = context.initSSE(wid_ht)
        var y = yAtomic.getAndIncrement()
        while (y < wid_ht) {
          val i0 = doubleArray(y)
          val row = pixels(y)
          var x = 0
          while (x < wid_ht / 8) {
            row(x) = context.calcPixels8(x, m128, i0)
            x += 1
          }
          y = yAtomic.getAndIncrement()
        }
        context.freem128d(m128)
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


}

@State(Scope.Thread)
class MandelbrotState {
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


