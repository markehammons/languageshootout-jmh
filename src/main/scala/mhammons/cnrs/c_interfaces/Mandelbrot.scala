package mhammons.cnrs.c_interfaces

import java.io.File
import java.nio.ByteBuffer

import org.graalvm.polyglot.{Source, TypeLiteral, Value}
import java.util.function.{Function => JFunction}

import mhammons.cnrs.InitValuesFun
import mhammons.cnrs.c_interfaces.Mandelbrot.src

import scala.language.higherKinds
import scala.reflect.ClassTag

@FunctionalInterface
trait CalcPixelsFun {
  def execute(x: Int, r: Value, i: Double, retArr: Array[Long]): Unit
}


@FunctionalInterface
trait CalcPixels8Fun {
  def execute(x: Int, r: Value, i0: Double): Byte
}

@FunctionalInterface
trait LoadInitFun {
  def execute(d: Double, i: Int): Unit
}

@FunctionalInterface
trait SSEInitFun {
  def execute(wid_ht: Long): Value
}

trait Mandelbrot {
  def mandelbrot(wid_ht: Long): Int
  def initSSE(wid_ht: Long): Value
  def initValues(arr: Array[Double], wid_ht: Long): Unit
  def freem128d(value: Value): Unit
  def genInitI(d: Double): Value
  def loadInit(d: Double, i: Int): Unit
  def calcPixels(b: Array[Byte], rowstart: Int, x: Int, r0: Value, initI: Double): Unit
  def calcPixels8(x: Int, r0: Value, i0: Double): Byte
}

object Mandelbrot {
  trait FN2Shim[A] {
    type B
    type C
  }
  type Aux[A,B0,C0] = FN2Shim[A] {type B = B0; type C = C0}
  implicit def fn2Shim[T,U]: Aux[T => U, T, U] = new FN2Shim[T => U] {
    type B = T
    type C = U
  }
  implicit def typeLiteralFN2[T,U]: TypeLiteral[JFunction[T,U]] = new TypeLiteral[JFunction[T, U]] {}
  implicit class ValueEnhanced(val value: Value) extends AnyVal {
    def to[F, T,U, D](clazz: Class[F])(implicit ev2: Aux[F,T,U],ev: TypeLiteral[JFunction[T,U]]) = (t: T) => value.as(ev)(t)
  }


  val src = Source.newBuilder("llvm", new File("target/Mandelbrot.bc")).build()

}

class SMandelbrot extends Sulong(src) with Mandelbrot  {

  val mandelbrotF = lib.getMember("mandelbrot")
  private val SSEInitF = lib.getMember("SSEinit").as(classOf[SSEInitFun])
  private val initValuesF = lib.getMember("initValues").as(classOf[InitValuesFun])
  private val freem128 = lib.getMember("freem128d")
  private val geninit_i = lib.getMember("genInitI")
  private val loadInitF = lib.getMember("loadInit").as(classOf[LoadInitFun])
  private val calc = lib.getMember("calcPixels").as(classOf[CalcPixelsFun])
  private val calcPixels8F = lib.getMember("calcPixels8").as(classOf[CalcPixels8Fun])

  override def initSSE(wid_ht: Long): Value = {
    SSEInitF.execute(wid_ht)
  }

  override def initValues(arr: Array[Double], wid_ht: Long): Unit = {
    initValuesF.execute(arr, wid_ht)
  }

  override def freem128d(value: Value) = {
    freem128.executeVoid(value)
  }

  override def genInitI(d: Double): Value = {
    geninit_i.execute(d.asInstanceOf[Object])
  }

  val retArray = Array.ofDim[Long](250)


  def close() = {
    context.close()
  }


  override def calcPixels(b: Array[Byte], rowstart: Int, x: Int, r0: Value, initI: Double): Unit = {
    calc.execute(x, r0, initI, retArray)
    var pos = rowstart
    var i = 0
    var j = 0
    var l = 0l
    while(i < x) {
      l = retArray(i)
      while(j < 8) {
        b(pos) = (l & 0xff).toByte
        l >>= 8
        j += 1
        pos += 1
      }
      j = 0
      i += 1
    }
  }

  override def mandelbrot(wid_ht: Long): Int = mandelbrotF.execute(wid_ht.asInstanceOf[Object]).asInt()

  override def calcPixels8(x: Int, r0: Value, i0: Double): Byte = {
    calcPixels8F.execute(x, r0, i0)
  }

  override def loadInit(d: Double, i: Int): Unit = {
    loadInitF.execute(d, i)
  }
}
