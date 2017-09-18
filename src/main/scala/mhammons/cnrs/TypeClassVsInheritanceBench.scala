package mhammons.cnrs

import java.util.concurrent.TimeUnit

import mhammons.cnrs.PolymorphismBench.BenchState
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

class PolymorphismBench {
  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime, Mode.SampleTime, Mode.Throughput))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def typeclassBench(state: BenchState, blackhole: Blackhole) = {
    blackhole.consume(state.aAble.add(state.aClass, 5))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime, Mode.SampleTime, Mode.Throughput))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def traitInheritance(state: BenchState, blackhole: Blackhole) = {
    blackhole.consume(state.aClass.add(5))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime, Mode.SampleTime, Mode.Throughput))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def abstractInheritance(state: BenchState, blackhole: Blackhole) = {
    blackhole.consume(state.abstractAdder.add(5))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime, Mode.SampleTime, Mode.Throughput))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def typeclassFunction(state: BenchState, blackhole: Blackhole) = {
    blackhole.consume(polymorphicFunction(state.aClass))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime, Mode.SampleTime, Mode.Throughput))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def traitFunction(state: BenchState, blackhole: Blackhole) = {
    blackhole.consume(polymorphicFunctionTrait(state.adder))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime, Mode.SampleTime, Mode.Throughput))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def abstractImplFunction(state: BenchState, blackhole: Blackhole) = {
    blackhole.consume(polymorphicFunctionAbstract(state.abstractAdder))
  }

  private def polymorphicFunctionTrait[T <: Adder](t: T) = {
    t.add(5)
  }

  private def polymorphicFunction[T](t: T)(implicit addable: Addable[T]) = {
    addable.add(t, 5)
  }

  private def polymorphicFunctionAbstract[T <: AbstractAdder](t: T) = t.add(5)

}

object PolymorphismBench {
  @State(Scope.Thread)
  class BenchState {
    val aAble = implicitly[Addable[AdderImpl]]
    val aClass = new AdderImpl(5)
    val adder: Adder = aClass
    val abstractAdder = new AbstractAdderImpl(5)
  }

}
