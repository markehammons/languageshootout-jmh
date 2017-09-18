package mhammons.cnrs

abstract class AbstractAdder(val i: Int) {
  def add(o: Int): Int = i + o
}
